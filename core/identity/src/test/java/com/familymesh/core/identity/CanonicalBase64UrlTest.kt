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
    fun rejectsWhitespace() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode(" ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode("\n")
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
    fun rejectsPlusSignInAlphabet() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode("abc+")
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
        val bytes = byteArrayOf(1, 2, 3)
        val canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val nonCanonical = canonical + "A"
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode(nonCanonical)
        }
    }

    @Test
    fun rejectsEmptyDecodedBytes() {
        assertThrows(IllegalArgumentException::class.java) {
            CanonicalBase64Url.decode(Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf()))
        }
    }
}
