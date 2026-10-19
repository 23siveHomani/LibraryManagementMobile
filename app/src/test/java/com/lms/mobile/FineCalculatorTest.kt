package com.lms.mobile

import com.lms.mobile.logic.FineCalculator
import com.lms.mobile.logic.FineCalculator.DAY_MS
import com.lms.mobile.logic.FineCalculator.Status
import org.junit.Assert.assertEquals
import org.junit.Test

class FineCalculatorTest {
    private val due = 1_000_000_000L

    @Test fun notLateWhenReturnedBeforeOrOnDueTime() {
        assertEquals(0, FineCalculator.daysOverdue(due, due - 5 * DAY_MS))
        assertEquals(0, FineCalculator.daysOverdue(due, due))
    }

    @Test fun lessThanOneFullDayLateIsNotCharged() {
        assertEquals(0, FineCalculator.daysOverdue(due, due + DAY_MS - 1))
    }

    @Test fun wholeDaysLateAreCounted() {
        assertEquals(1, FineCalculator.daysOverdue(due, due + DAY_MS))
        assertEquals(5, FineCalculator.daysOverdue(due, due + 5 * DAY_MS + 1000))
    }

    @Test fun fineIsFiveRandPerDay() {
        assertEquals(0, FineCalculator.fineForDays(0))
        assertEquals(0, FineCalculator.fineForDays(-3))
        assertEquals(5, FineCalculator.fineForDays(1))
        assertEquals(25, FineCalculator.fineForDays(5))
    }

    @Test fun fineIsCappedAtR150() {
        assertEquals(150, FineCalculator.fineForDays(30))
        assertEquals(150, FineCalculator.fineForDays(31))
        assertEquals(150, FineCalculator.fineForDays(365))
    }

    @Test fun fineFromDates() {
        assertEquals(45, FineCalculator.fine(due, due + 9 * DAY_MS))
        assertEquals(0, FineCalculator.fine(due, due - DAY_MS))
    }

    @Test fun statusReflectsDueDateAndReminderWindow() {
        val now = 5_000_000_000L
        assertEquals(Status.RETURNED, FineCalculator.status(now - DAY_MS, now - 2 * DAY_MS, now, 2))
        assertEquals(Status.OVERDUE, FineCalculator.status(now - 1, null, now, 2))
        assertEquals(Status.DUE_SOON, FineCalculator.status(now + 2 * DAY_MS, null, now, 2))
        assertEquals(Status.ON_TIME, FineCalculator.status(now + 3 * DAY_MS, null, now, 2))
        assertEquals(Status.DUE_SOON, FineCalculator.status(now + 3 * DAY_MS, null, now, 5))
    }
}
