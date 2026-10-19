package com.lms.mobile

import com.lms.mobile.logic.LoginThrottle
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginThrottleTest {
    @Test fun noLockBeforeFiveFailures() {
        for (n in 0..4) assertEquals(0L, LoginThrottle.lockUntil(n, 1000L))
    }

    @Test fun locksForThirtySecondsOnTheFifthFailure() {
        assertEquals(1000L + 30_000L, LoginThrottle.lockUntil(5, 1000L))
        assertEquals(1000L + 30_000L, LoginThrottle.lockUntil(9, 1000L))
    }

    @Test fun secondsLeftCountsDownAndNeverGoesNegative() {
        assertEquals(30, LoginThrottle.secondsLeft(31_000L, 1000L))
        assertEquals(1, LoginThrottle.secondsLeft(31_000L, 30_500L))
        assertEquals(0, LoginThrottle.secondsLeft(31_000L, 31_000L))
        assertEquals(0, LoginThrottle.secondsLeft(0L, 31_000L))
    }
}
