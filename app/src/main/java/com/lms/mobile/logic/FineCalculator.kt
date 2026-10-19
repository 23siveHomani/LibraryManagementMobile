package com.lms.mobile.logic

import com.lms.mobile.Config

/** Loan status and late-fee rules. Pure functions so they can be unit tested without a device. */
object FineCalculator {
    const val DAY_MS = 24L * 60L * 60L * 1000L

    /** Whole days that [nowMillis] is past [dueMillis]; 0 when the book is not late yet. */
    fun daysOverdue(dueMillis: Long, nowMillis: Long): Int {
        if (nowMillis <= dueMillis) return 0
        return ((nowMillis - dueMillis) / DAY_MS).toInt()
    }

    /** Fine in rand: R5 per whole day late, capped at R150. */
    fun fineForDays(days: Int): Int {
        if (days <= 0) return 0
        return minOf(days * Config.FINE_PER_DAY_RAND, Config.FINE_CAP_RAND)
    }

    fun fine(dueMillis: Long, returnedOrNowMillis: Long): Int =
        fineForDays(daysOverdue(dueMillis, returnedOrNowMillis))

    enum class Status { RETURNED, OVERDUE, DUE_SOON, ON_TIME }

    fun status(dueMillis: Long, returnedMillis: Long?, nowMillis: Long, reminderDays: Int): Status = when {
        returnedMillis != null -> Status.RETURNED
        nowMillis > dueMillis -> Status.OVERDUE
        dueMillis - nowMillis <= reminderDays * DAY_MS -> Status.DUE_SOON
        else -> Status.ON_TIME
    }
}
