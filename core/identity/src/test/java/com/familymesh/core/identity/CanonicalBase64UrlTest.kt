package com.familymesh.core.identity

import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test

class CanonicalBase64UrlTest {
    @Test
    fun acceptsCanonicalUnpaddedBase64Url() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        assertArrayEquals(bytes, CanonicalBase64Url.decode(encoded))
    }

    @Test
    fun rejectsPaddedAliasOfCanonicalValue() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val padding = (4 - canonical.length % 4) % 4
        val padded = canonical + "=".repeat(padding)
        try {
            CanonicalBase64Url.decode(padded)
            fail("Expected padded Base64URL alias to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun rejectsNonBase64UrlAlphabet() {
        try {
            CanonicalBase64Url.decode("ab+c/")
            fail("Expected standard Base64 alphabet to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
