package com.example.hamkketa.domain.ride

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCalculatorTest {
    @Test fun expenseBeforeAlightingKeepsPassengerEvenAtSameOdometer() {
        val record = trip(listOf(RiderEvent("a", 0.0, true), RiderEvent("a", 10.0, false)))
            .copy(
                group = group.copy(costs = listOf(CostItem("toll", "toll"))),
                expenses = listOf(TripExpense("toll", 1000, 10.0, listOf("a")))
            )
        val result = TripCalculator.calculate(record)
        assertEquals(500L, result.amounts["a"])
        assertEquals(500L, result.amounts["driver"])
    }

    @Test fun planningDocumentTwentyKilometerExample() {
        val record = TripRecord(
            "example",
            group,
            TripSettings(12.0, 1650.0),
            0,
            0.0,
            20.0,
            events = listOf(RiderEvent("a", 0.0, true), RiderEvent("b", 8.0, true))
        )
        assertEquals(
            mapOf("driver" to 1100L, "a" to 1100L, "b" to 550L),
            TripCalculator.calculate(record).amounts
        )
    }
    private val group = RideGroup("g", "test", listOf(RideMember("a", "A"), RideMember("b", "B")))
    private val settings = TripSettings(kmPerLiter = 10.0, wonPerLiter = 1000.0)
    private fun trip(events: List<RiderEvent>) =
        TripRecord("t", group, settings, 0, 0.0, 30.0, events = events)

    @Test fun lateBoardingAndEarlyAlightingOnlyChargeRiddenDistance() {
        val result = TripCalculator.calculate(
            trip(
                listOf(
                    RiderEvent("a", 0.0, true),
                    RiderEvent("b", 10.0, true),
                    RiderEvent("a", 20.0, false)
                )
            )
        )
        assertEquals(3000L, result.amounts.values.sum())
        assertEquals(20.0, result.distanceByMember.getValue("a"), 0.00001)
        assertEquals(20.0, result.distanceByMember.getValue("b"), 0.00001)
        assertEquals(830L, result.amounts.getValue("a"))
        assertEquals(1340L, result.amounts.getValue("driver"))
        assertEquals(830L, result.amounts.getValue("b"))
    }

    @Test fun backdatedDisconnectDoesNotEraseAnotherBoarding() {
        val result = TripCalculator.calculate(
            trip(
                listOf(
                    RiderEvent("a", 0.0, true),
                    RiderEvent("b", 15.0, true),
                    RiderEvent("a", 10.0, false)
                )
            )
        )
        assertEquals(10.0, result.distanceByMember.getValue("a"), 0.0)
        assertEquals(15.0, result.distanceByMember.getValue("b"), 0.0)
        assertEquals(500L, result.amounts.getValue("a"))
    }

    @Test fun passengerOnlyRuleAndExemptionsApplyTogether() {
        val record = trip(listOf(RiderEvent("a", 0.0, true), RiderEvent("b", 0.0, true)))
            .copy(group = group.copy(rule = SplitRule.Passengers, exemptIds = setOf("a")))
        assertEquals(
            mapOf("driver" to 0L, "a" to 0L, "b" to 3000L),
            TripCalculator.calculate(record).amounts
        )
    }

    @Test fun fuelExcludedAndExpenseAllocatedAtItsOwnBoundary() {
        val record = trip(listOf(RiderEvent("a", 0.0, true), RiderEvent("a", 10.0, false)))
            .copy(
                group = group.copy(
                    costs = listOf(CostItem("fuel", "fuel", false), CostItem("toll", "toll"))
                ),
                expenses = listOf(TripExpense("toll", 500, 5.0), TripExpense("toll", 500, 20.0))
            )
        val amounts = TripCalculator.calculate(record).amounts
        assertEquals(250L, amounts["a"])
        assertEquals(750L, amounts["driver"])
    }

    @Test fun tinySegmentsDoNotLoseMoneyBeforeFinalRounding() {
        val record = trip((0..100).map { RiderEvent("a", it / 100.0, it % 2 == 0) })
            .copy(endKm = 1.0, settings = settings.copy(kmPerLiter = 3.0))
        val amounts = TripCalculator.calculate(record).amounts
        assertEquals(330L, amounts.values.sum())
        assertTrue(amounts.values.all { it % 10 == 0L })
    }

    @Test fun disconnectedPassengerRetainsOriginalDistanceAfterGrace() {
        val policy = TripBoardingPolicy(settings)
        policy.doorCycle(0)
        assertEquals(listOf(RiderEvent("a", 0.0, true)), policy.connect("a", -50, 0.0, 1))
        policy.disconnect("a", 10.0, 1000)
        assertTrue(policy.tick(30_999).isEmpty())
        assertEquals(listOf(RiderEvent("a", 10.0, false)), policy.tick(31_000))
        assertTrue(policy.tick(40_000).isEmpty())
    }

    @Test fun briefDisconnectAndAddressIndependentReconnectKeepOneRide() {
        val policy = TripBoardingPolicy(settings)
        policy.doorCycle(0)
        policy.connect("a", -50, 0.0, 1)
        policy.disconnect("a", 5.0, 1000)
        assertTrue(policy.connect("a", -50, 10.0, 2000).isEmpty())
        assertTrue(policy.tick(40_000).isEmpty())
    }

    @Test fun weakSignalOrMissingDoorDoesNotBoard() {
        val policy = TripBoardingPolicy(settings)
        assertTrue(policy.connect("a", -50, 0.0, 1).isEmpty())
        policy.doorCycle(2)
        assertTrue(policy.connect("a", -90, 0.0, 3).isEmpty())
        assertTrue(policy.connect("a", -50, 0.0, 60_003).isEmpty())
    }

    @Test fun multiplePendingAlightsAreConfirmedIndependently() {
        val policy = TripBoardingPolicy(settings)
        policy.doorCycle(0)
        policy.connect("a", -50, 0.0, 1)
        policy.connect("b", -50, 0.0, 1)
        policy.disconnect("a", 5.0, 1000)
        policy.disconnect("b", 10.0, 2000)
        assertEquals(listOf(RiderEvent("a", 5.0, false)), policy.tick(31_000))
        assertEquals(listOf(RiderEvent("b", 10.0, false)), policy.tick(32_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun reversedOdometerRejected() {
        TripCalculator.calculate(trip(emptyList()).copy(endKm = -1.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroEfficiencyRejected() {
        TripCalculator.calculate(trip(emptyList()).copy(settings = settings.copy(kmPerLiter = 0.0)))
    }
}
