package com.example.hamkketa.domain.ride

import kotlin.math.roundToLong

enum class SplitRule(val title: String, val description: String) {
    Equal("운전자 포함 균등 분할", "같은 구간에 탄 모두가 자기 몫을 부담해요."),
    Passengers("동승자끼리 나누기", "운전자를 제외한 동승자가 나눠 부담해요."),
    Exempt("특정 동승자 면제", "선택한 동승자를 제외하고 나눠요.")
}

data class CostSegment(val distanceKm: Double, val costWon: Long, val passengerIds: List<String>)

object SettlementCalculator {
    const val DRIVER = "driver"

    fun calculate(
        segments: List<CostSegment>,
        rule: SplitRule,
        exempt: Set<String> = emptySet()
    ): Map<String, Long> {
        val amounts = linkedMapOf<String, Double>()
        segments.forEach { segment ->
            require(
                segment.costWon >= 0 && segment.distanceKm.isFinite() && segment.distanceKm >= 0
            )
            val riders = (listOf(DRIVER) + segment.passengerIds).distinct()
            val payers = riders.filter {
                when (rule) {
                    SplitRule.Equal -> true
                    SplitRule.Passengers -> it != DRIVER
                    SplitRule.Exempt -> it == DRIVER || it !in exempt
                }
            }.ifEmpty { listOf(DRIVER) }
            riders.forEach { amounts.putIfAbsent(it, 0.0) }
            payers.forEach {
                amounts[it] =
                    amounts.getValue(it) + segment.costWon.toDouble() / payers.size
            }
        }
        val result = amounts.mapValues { (_, value) -> (value / 10).toLong() * 10 }.toMutableMap()
        val total = (segments.sumOf { it.costWon }.toDouble() / 10).roundToLong() * 10
        val remaining = ((total - result.values.sum()) / 10).toInt()
        amounts.keys.sortedByDescending { amounts.getValue(it) - result.getValue(it) }
            .take(remaining).forEach { result[it] = result.getValue(it) + 10 }
        return result
    }
}
