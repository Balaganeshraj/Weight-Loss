package com.example.wellbeing

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class DomainTest {
    @Test fun bmiUsesMetricUnits() { assertEquals(24.221, WellnessMath.bmi(70.0, 170.0), 0.001) }
    @Test(expected = IllegalArgumentException::class) fun bmiRejectsZeroHeight() { WellnessMath.bmi(70.0, 0.0) }
    @Test(expected = IllegalArgumentException::class) fun bmiRejectsNonFiniteWeight() { WellnessMath.bmi(Double.NaN, 170.0) }
    @Test fun kilojoulesConvertToKcal() { assertEquals(100.0, WellnessMath.kcal(418.4), 0.00001) }
    @Test fun portionsScaleDeterministically() { assertEquals(195.0, WellnessMath.portion(130.0, 150.0), 0.00001) }
    @Test(expected = IllegalArgumentException::class) fun negativePortionRejected() { WellnessMath.portion(130.0, -10.0) }
    @Test fun hydrationBounds() {
        assertFalse(WellnessMath.valid(Entry(kind = Kind.WATER, amount = -1.0)))
        assertFalse(WellnessMath.valid(Entry(kind = Kind.WATER, amount = Double.POSITIVE_INFINITY)))
        assertTrue(WellnessMath.valid(Entry(kind = Kind.WATER, amount = 250.0)))
    }
    @Test fun foodRequiresNameAndPlausiblePortion() {
        assertFalse(WellnessMath.valid(Entry(kind = Kind.FOOD, amount = 100.0)))
        assertFalse(WellnessMath.valid(Entry(kind = Kind.FOOD, title = "Rice", amount = 0.0)))
        assertTrue(WellnessMath.valid(Entry(kind = Kind.FOOD, title = "Rice", amount = 150.0, energy = 195.0)))
    }
    @Test fun dateUsesUserTimezone() {
        val entry = Entry(kind = Kind.WATER, at = Instant.parse("2026-10-26T20:00:00Z").toEpochMilli())
        assertTrue(WellnessMath.onDate(entry, LocalDate.parse("2026-10-27"), ZoneId.of("Asia/Colombo")))
        assertFalse(WellnessMath.onDate(entry, LocalDate.parse("2026-10-27"), ZoneOffset.UTC))
    }
    @Test fun dstTransitionDoesNotDropEntries() {
        val entry = Entry(kind = Kind.ACTIVITY, at = Instant.parse("2026-11-01T06:30:00Z").toEpochMilli())
        assertTrue(WellnessMath.onDate(entry, LocalDate.parse("2026-11-01"), ZoneId.of("America/New_York")))
    }
    @Test fun timerUsesMonotonicTime() {
        val timer = TimerState(5000, 10000, 3)
        assertEquals(12000, timer.elapsed(17000, 3))
        assertEquals(12000, timer.pause(17000, 3).elapsed(999999, 3))
    }
    @Test fun timerRebootDoesNotInventTime() { assertEquals(5000, TimerState(5000, 10000, 3).elapsed(500, 4)) }
    @Test fun timerNeverAddsNegativeTime() { assertEquals(5000, TimerState(5000, 10000, 3).elapsed(9000, 3)) }
    @Test fun allergenExclusionIsHardFilter() {
        val allowed = recipes.filter { it.allergens.intersect(setOf("Peanuts")).isEmpty() }
        assertFalse(allowed.any { it.description.contains("peanut", true) })
        assertEquals(2, allowed.size)
    }
}
