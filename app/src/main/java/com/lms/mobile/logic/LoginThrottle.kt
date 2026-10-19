package com.lms.mobile.logic

import com.lms.mobile.Config

/** Slows down password guessing: after 5 failed logins the form locks for 30 seconds. */
object LoginThrottle {
    /** Returns the time (epoch millis) until which login is blocked, or 0 when not blocked. */
    fun lockUntil(failedAttempts: Int, nowMillis: Long): Long =
        if (failedAttempts >= Config.MAX_FAILED_LOGINS) nowMillis + Config.LOCKOUT_SECONDS * 1000L else 0L

    fun secondsLeft(lockedUntil: Long, nowMillis: Long): Int =
        if (lockedUntil <= nowMillis) 0 else (((lockedUntil - nowMillis) + 999) / 1000).toInt()
}
