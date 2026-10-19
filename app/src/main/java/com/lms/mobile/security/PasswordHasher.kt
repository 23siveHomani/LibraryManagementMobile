package com.lms.mobile.security

import com.lms.mobile.Config
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2-HMAC-SHA256 password hashing. Plain-text passwords are never stored.
 *
 * Stored format:  pbkdf2_sha256:<iterations>:<salt hex>:<hash hex>
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val TAG = "pbkdf2_sha256"
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    fun encode(password: String, iterations: Int = Config.PBKDF2_ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val hash = derive(password.toCharArray(), salt, iterations)
        return listOf(TAG, iterations.toString(), toHex(salt), toHex(hash)).joinToString(":")
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 4 || parts[0] != TAG) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = fromHex(parts[2]) ?: return false
        val expected = fromHex(parts[3]) ?: return false
        if (iterations < 1) return false
        val actual = derive(password.toCharArray(), salt, iterations)
        // Constant-time comparison so timing does not leak how many bytes matched.
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun fromHex(hex: String): ByteArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            if (hi < 0 || lo < 0) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
