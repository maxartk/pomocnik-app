package cz.kovmak.pomocnik.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SapDurationCalculatorTest {
    @Test
    fun calculatesDurationFromNotificationToFailureEnd() {
        assertEquals(
            2.52,
            SapDurationCalculator.hours("21.07.2026", "15:29", "21.07.2026", "18:00"),
            0.001
        )
    }

    @Test
    fun supportsCrossMidnightAndOutagesLongerThanOneDay() {
        assertEquals(
            1.5,
            SapDurationCalculator.hours("21.07.2026", "23:30", "22.07.2026", "01:00"),
            0.001
        )
        assertEquals(
            26.5,
            SapDurationCalculator.hours("21.07.2026", "23:30", "23.07.2026", "02:00"),
            0.001
        )
    }

    @Test
    fun rejectsInvalidOrBackwardsIntervals() {
        assertEquals(0.0, SapDurationCalculator.hours("", "15:29", "21.07.2026", "18:00"), 0.001)
        assertEquals(0.0, SapDurationCalculator.hours("31.02.2026", "15:29", "21.07.2026", "18:00"), 0.001)
        assertEquals(0.0, SapDurationCalculator.hours("21.07.2026", "25:00", "21.07.2026", "18:00"), 0.001)
        assertEquals(0.0, SapDurationCalculator.hours("22.07.2026", "15:29", "21.07.2026", "18:00"), 0.001)
    }

    @Test
    fun suggestsEndDateWhileUserOnlyEntersTheEndTime() {
        assertEquals("21.07.2026", SapDurationCalculator.suggestEndDate("21.07.2026", "15:29", "18:00"))
        assertEquals("22.07.2026", SapDurationCalculator.suggestEndDate("21.07.2026", "23:30", "01:00"))
        assertEquals("", SapDurationCalculator.suggestEndDate("31.02.2026", "15:29", "18:00"))
    }

    @Test
    fun resolvesAutomaticEndDateAgainWhenEndTimeIsCorrected() {
        assertEquals(
            "21.07.2026",
            SapDurationCalculator.resolveEndDate(
                notificationDate = "21.07.2026",
                notificationTime = "23:30",
                failureEndTime = "23:45",
                currentEndDate = "22.07.2026",
                endDateManuallyEdited = false
            )
        )
        assertEquals(
            "23.07.2026",
            SapDurationCalculator.resolveEndDate(
                notificationDate = "21.07.2026",
                notificationTime = "23:30",
                failureEndTime = "23:45",
                currentEndDate = "23.07.2026",
                endDateManuallyEdited = true
            )
        )
    }

    @Test
    fun exposesStrictDateAndTimeValidationForUiGates() {
        assertTrue(SapDurationCalculator.isValidDate("21.07.2026"))
        assertFalse(SapDurationCalculator.isValidDate("31.02.2026"))
        assertTrue(SapDurationCalculator.isValidTime("23:59"))
        assertFalse(SapDurationCalculator.isValidTime("24:00"))
    }
}
