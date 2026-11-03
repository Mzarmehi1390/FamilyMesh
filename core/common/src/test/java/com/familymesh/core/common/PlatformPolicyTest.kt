package com.familymesh.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PlatformPolicyTest {
    @Test
    fun minimumSupportedAndroidApi_isAndroid12() {
        assertEquals(31, PlatformPolicy.MIN_SUPPORTED_ANDROID_API)
    }
}
