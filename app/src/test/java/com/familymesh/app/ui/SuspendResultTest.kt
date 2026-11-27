package com.familymesh.app.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendResultTest {
    @Test
    fun cancellationIsRethrown() {
        val cancellation = CancellationException("screen stopped")

        val thrown = try {
            runBlocking {
                runSuspendCatching<Unit> { throw cancellation }
            }
            null
        } catch (failure: CancellationException) {
            failure
        }

        assertSame(cancellation, thrown)
    }

    @Test
    fun ordinaryExceptionIsReturnedAsFailure() = runBlocking {
        val failure = IllegalStateException("storage unavailable")

        val result = runSuspendCatching<Unit> { throw failure }

        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun successfulValueIsPreserved() = runBlocking {
        val result = runSuspendCatching { "saved" }

        assertTrue(result.isSuccess)
        assertSame("saved", result.getOrNull())
    }
}
