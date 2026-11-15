package com.example.hamkketa.domain.ride

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementCalculatorTest {
    private val segments =
        listOf(CostSegment(8.0, 1200, listOf("a")), CostSegment(12.0, 2100, listOf("a", "b")))

    @Test fun latePassengerOnlyPaysForTheirSegment() {
        assertEquals(
            mapOf("driver" to 1300L, "a" to 1300L, "b" to 700L),
            SettlementCalculator.calculate(segments, SplitRule.Equal)
        )
    }

    @Test fun passengersOnlyDoesNotChargeDriver() {
        assertEquals(
            mapOf("driver" to 0L, "a" to 2250L, "b" to 1050L),
            SettlementCalculator.calculate(segments, SplitRule.Passengers)
        )
    }

    @Test fun exemptionStillConservesCost() {
        assertEquals(
            mapOf("driver" to 2250L, "a" to 0L, "b" to 1050L),
            SettlementCalculator.calculate(segments, SplitRule.Exempt, setOf("a"))
        )
    }

    @Test fun roundingDoesNotCreateOrLoseTens() {
        val result = SettlementCalculator.calculate(
            listOf(CostSegment(1.0, 100, listOf("a", "b"))),
            SplitRule.Equal
        )
        assertEquals(100L, result.values.sum())
        assertTrue(result.values.all { it % 10 == 0L })
        assertEquals(40L, result["driver"])
    }

    @Test fun driverAbsorbsSegmentsWithNoPayingPassengers() {
        assertEquals(
            mapOf("driver" to 110L),
            SettlementCalculator.calculate(
                listOf(CostSegment(1.0, 106, emptyList())),
                SplitRule.Passengers
            )
        )
    }

    @Test fun duplicatePassengerIdsAreNotDoubleCharged() {
        assertEquals(
            mapOf("driver" to 50L, "a" to 50L),
            SettlementCalculator.calculate(
                listOf(CostSegment(1.0, 100, listOf("a", "a"))),
                SplitRule.Equal
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeCost() {
        SettlementCalculator.calculate(listOf(CostSegment(1.0, -1, emptyList())), SplitRule.Equal)
    }
}
