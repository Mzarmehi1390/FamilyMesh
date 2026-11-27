package com.familymesh.app.ui

import kotlinx.coroutines.CancellationException

/**
 * Captures ordinary failures from a suspending operation without converting
 * coroutine cancellation into an application error.
 */
internal suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
