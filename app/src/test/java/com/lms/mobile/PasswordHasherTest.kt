package com.lms.mobile

import com.lms.mobile.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    private val fast = 1000 // fewer iterations keep the tests quick; the app uses Config.PBKDF2_ITERATIONS

    @Test
    fun correctPasswordVerifies() {
        val stored = PasswordHasher.encode("Passw0rd!", fast)
        assertTrue(PasswordHasher.verify("Passw0rd!", stored))
    }

    @Test
    fun wrongPasswordIsRejected() {
        val stored = PasswordHasher.encode("Passw0rd!", fast)
        assertFalse(PasswordHasher.verify("passw0rd!", stored))
        assertFalse(PasswordHasher.verify("", stored))
    }

    @Test
    fun plainTextIsNeverStored() {
        val stored = PasswordHasher.encode("Passw0rd!", fast)
        assertFalse(stored.contains("Passw0rd!"))
        assertTrue(stored.startsWith("pbkdf2_sha256:1000:"))
    }

    @Test
    fun sameCharacterSetGivesDifferentHashesBecauseOfSalt() {
        val a = PasswordHasher.encode("Passw0rd!", fast)
        val b = PasswordHasher.encode("Passw0rd!", fast)
        assertNotEquals(a, b)
        assertTrue(PasswordHasher.verify("Passw0rd!", a))
        assertTrue(PasswordHasher.verify("Passw0rd!", b))
    }

    @Test
    fun malformedStoredValuesAreRejectedWithoutCrashing() {
        assertFalse(PasswordHasher.verify("x", "GOOGLE"))
        assertFalse(PasswordHasher.verify("x", ""))
        assertFalse(PasswordHasher.verify("x", "pbkdf2_sha256:abc:00:00"))
        assertFalse(PasswordHasher.verify("x", "pbkdf2_sha256:1000:zz:zz"))
        assertFalse(PasswordHasher.verify("x", "other:1000:aa:bb"))
    }

    @Test
    fun hexRoundTrip() {
        val bytes = byteArrayOf(0, 1, 15, 16, 127, -1, -128)
        val hex = PasswordHasher.toHex(bytes)
        assertEquals("00010f107fff80", hex)
        assertTrue(bytes.contentEquals(PasswordHasher.fromHex(hex)!!))
        assertNull(PasswordHasher.fromHex("abc"))
        assertNull(PasswordHasher.fromHex("zz"))
    }
}
