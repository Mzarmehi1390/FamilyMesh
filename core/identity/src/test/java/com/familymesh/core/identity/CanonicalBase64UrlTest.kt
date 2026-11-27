package com.familymesh.core.identity

import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalBase64UrlTest {
    @Test
    fun acceptsCanonicalUnpaddedBase64Url() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        assertArrayEquals(bytes, CanonicalBase64Url.decode(encoded))
    }

    @Test
    fun rejectsEmptyString() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode("")
        }
    }

    @Test
    fun rejectsPaddedAliasOfCanonicalValue() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val padding = (4 - canonical.length % 4) % 4
        val padded = canonical + "=".repeat(padding)
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode(padded)
        }
    }

    @Test
    fun rejectsNonBase64UrlAlphabet() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode("ab+c/")
        }
    }

    @Test
    fun rejectsInvalidCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode("abc!")
        }
    }

    @Test
    fun rejectsNonCanonicalEncoding() {
        // A value that decodes but is not the canonical unpadded form (e.g. with different padding or length issues)
        val bytes = byteArrayOf(1, 2, 3)
        val canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        // Force a non-canonical by adding extra valid chars that don't match round-trip
        val nonCanonical = canonical + "A" // may or may not be valid; expect rejection on canonical check or alphabet
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode(nonCanonical)
        }
    }

    @Test
    fun acceptsEmptyDecodedBytesEdge() {
        // Zero-length is empty string which is rejected; test a minimal valid
        val bytes = byteArrayOf()
        // Empty encoded is rejected by require(isNotEmpty)
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
        }
    }
}
