package com.example.hamkketa.domain.ride

import java.math.BigDecimal
import java.math.RoundingMode

data class TripSettings(
    val kmPerLiter: Double = 12.0,
    val wonPerLiter: Double = 1700.0,
    val simulation: Boolean = true,
    val rssiThreshold: Int = -65,
    val graceMs: Long = 30_000
) {
    fun validate() {
        require(kmPerLiter.isFinite() && kmPerLiter > 0 && kmPerLiter <= 100)
        require(wonPerLiter.isFinite() && wonPerLiter > 0 && wonPerLiter <= 100_000)
        require(rssiThreshold in -100..-20 && graceMs in 1_000..300_000)
    }
}

data class RiderEvent(val memberId: String, val km: Double, val joined: Boolean)
data class TripExpense(
    val costId: String,
    val won: Long,
    val km: Double,
    val passengerIds: List<String>? = null
)
data class TripRecord(
    val id: String,
    val group: RideGroup,
    val settings: TripSettings,
    val startedAt: Long,
    val startKm: Double,
    val endKm: Double = startKm,
    val endedAt: Long? = null,
    val events: List<RiderEvent> = emptyList(),
    val expenses: List<TripExpense> = emptyList(),
    val interrupted: Boolean = false
)
data class TripResult(
    val segments: List<CostSegment>,
    val amounts: Map<String, Long>,
    val distanceByMember: Map<String, Double>
)

fun TripRecord.activePassengerIds(): Set<String> {
    val active = linkedSetOf<String>()
    events.withIndex().sortedWith(
        compareBy<IndexedValue<RiderEvent>> {
            it.value.km
        }.thenBy { it.index }
    )
        .forEach { (_, event) ->
            if (event.joined) active.add(event.memberId) else active.remove(event.memberId)
        }
    return active
}

/** Replay confirmed events in distance order, including backdated disconnects. */
object TripCalculator {
    fun calculate(trip: TripRecord): TripResult {
        trip.settings.validate()
        require(trip.startKm.isFinite() && trip.endKm.isFinite() && trip.endKm >= trip.startKm)
        val validIds = trip.group.members.map { it.id }.toSet()
        val events = trip.events.withIndex().sortedWith(
            compareBy<IndexedValue<RiderEvent>> { it.value.km }.thenBy { it.index }
        )
        require(
            events.all {
                it.value.memberId in validIds &&
                    it.value.km in trip.startKm..trip.endKm
            }
        )
        val boundaries = (
            listOf(trip.startKm, trip.endKm) + events.map { it.value.km } +
                trip.expenses.map { it.km }
            ).distinct().sorted()
        require(trip.expenses.all { it.won >= 0 && it.km in trip.startKm..trip.endKm })
        val riders = linkedSetOf<String>()
        val precise = mutableListOf<Pair<List<String>, BigDecimal>>()
        val segments = mutableListOf<CostSegment>()
        val distances = linkedMapOf(SettlementCalculator.DRIVER to (trip.endKm - trip.startKm))
        trip.group.members.forEach { distances[it.id] = 0.0 }
        val fuel = trip.group.costs.any { it.id == "fuel" && it.included }
        boundaries.forEachIndexed { index, km ->
            events.filter { it.value.km == km }.forEach { (_, event) ->
                if (event.joined) riders.add(event.memberId) else riders.remove(event.memberId)
            }
            val distance = boundaries.getOrNull(index + 1)?.minus(km) ?: 0.0
            val cost = if (fuel) {
                BigDecimal.valueOf(distance).multiply(BigDecimal.valueOf(trip.settings.wonPerLiter))
                    .divide(BigDecimal.valueOf(trip.settings.kmPerLiter), 12, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            trip.expenses.filter { it.km == km }.forEach { expense ->
                if (trip.group.costs.any { it.id == expense.costId && it.included }) {
                    // A parked car can charge an expense and alight a rider at the same km.
                    // Keep the participants present when the expense was entered.
                    val participants = expense.passengerIds?.distinct() ?: riders.toList()
                    require(participants.all { it in validIds })
                    precise.add(participants to BigDecimal.valueOf(expense.won))
                    segments.add(CostSegment(0.0, expense.won, participants))
                }
            }
            riders.forEach { distances[it] = distances.getValue(it) + distance }
            if (distance > 0 || cost.signum() > 0) {
                segments.add(
                    CostSegment(
                        distance,
                        cost.setScale(0, RoundingMode.HALF_UP).toLong(),
                        riders.toList()
                    )
                )
                precise.add(riders.toList() to cost)
            }
        }
        val amounts = linkedMapOf<String, BigDecimal>()
        (listOf(SettlementCalculator.DRIVER) + validIds).forEach { amounts[it] = BigDecimal.ZERO }
        precise.forEach { (passengers, cost) ->
            val payers = (listOf(SettlementCalculator.DRIVER) + passengers).filter {
                (trip.group.rule != SplitRule.Passengers || it != SettlementCalculator.DRIVER) &&
                    (it == SettlementCalculator.DRIVER || it !in trip.group.exemptIds)
            }.ifEmpty { listOf(SettlementCalculator.DRIVER) }
            payers.forEach { id ->
                amounts[id] =
                    amounts.getValue(
                        id
                    ).add(cost.divide(BigDecimal(payers.size), 12, RoundingMode.HALF_UP))
            }
        }
        val ten = BigDecimal.TEN
        val result = amounts.mapValues {
            it.value.divide(ten, 0, RoundingMode.DOWN).toLong() * 10
        }.toMutableMap()
        val total = precise.fold(BigDecimal.ZERO) { acc, entry -> acc.add(entry.second) }
            .divide(ten, 0, RoundingMode.HALF_UP).toLong() * 10
        val remainder = ((total - result.values.sum()) / 10).toInt()
        amounts.keys.sortedByDescending {
            amounts.getValue(it).subtract(BigDecimal.valueOf(result.getValue(it)))
        }
            .take(remainder).forEach { result[it] = result.getValue(it) + 10 }
        return TripResult(segments, result, distances)
    }
}

/** No seat identity is inferred. A recent door cycle and a strong RSSI are required for a new rider. */
class TripBoardingPolicy(private val settings: TripSettings) {
    private data class Pending(val km: Double, val time: Long)
    private val active = mutableSetOf<String>()
    private val pending = mutableMapOf<String, Pending>()
    private var doorCycleAt: Long? = null

    fun doorCycle(now: Long) {
        doorCycleAt = now
    }

    fun connect(id: String, rssi: Int, km: Double, now: Long): List<RiderEvent> {
        val output = tick(now).toMutableList()
        if (id in active && pending.remove(id) != null) return output
        if (id in active) return output
        val door = doorCycleAt
        if (rssi >= settings.rssiThreshold &&
            rssi <= 0 &&
            door != null &&
            now - door in 0..60_000
        ) {
            active.add(id)
            output.add(RiderEvent(id, km, true))
        }
        return output
    }

    fun disconnect(id: String, km: Double, now: Long) {
        if (id in active) pending.putIfAbsent(id, Pending(km, now))
    }

    fun tick(now: Long): List<RiderEvent> = pending.filterValues {
        now - it.time >= settings.graceMs
    }
        .map { (id, point) ->
            active.remove(id)
            pending.remove(id)
            RiderEvent(id, point.km, false)
        }

    fun pendingIds(): Set<String> = pending.keys.toSet()
}
