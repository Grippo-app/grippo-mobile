package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.*
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

class MessagingTest {
    @Test fun staleWakeupDoesNotRetryFailedCallbackButANewCallbackCanRetry() = runTest {
        var attempts = 0
        val core = core(UnconfinedTestDispatcher(testScheduler)) {
            attempts++
            error("Fixture handler failure")
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            core.token.collect { result ->
                if (result is PushTokenResult.Available && result.token.value == "first") {
                    core.onRegistrationToken("second")
                }
            }
        }
        try {
            core.onRegistrationToken("first")
            runCurrent()
            assertEquals(1, attempts, "A wakeup from the older callback retried the latest failed callback")
            assertEquals(TokenSyncStatus.Failed, core.syncStatus.value)
            core.onRegistrationToken("second")
            runCurrent()
            assertEquals(2, attempts)
        } finally { core.close() }
    }

    @Test fun reentrantTokenObserverCannotReplaceANewerCallbackWithAnOlderToken() = runTest {
        val delivered = mutableListOf<PushToken>()
        val core = core(StandardTestDispatcher(testScheduler)) { delivered += it }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            core.token.collect { result ->
                if (result is PushTokenResult.Available && result.token.value == "first") {
                    core.onRegistrationToken("second")
                }
            }
        }
        try {
            core.onRegistrationToken("first")
            advanceUntilIdle()
            assertEquals("second", (core.token.value as PushTokenResult.Available).token.value)
            assertEquals(listOf(PushToken("second")), delivered)
            assertEquals(TokenSyncStatus.Synced, core.syncStatus.value)
        } finally { core.close() }
    }

    private fun core(dispatcher: CoroutineDispatcher, action: suspend (PushToken) -> Unit = {}): DefaultMessaging =
        DefaultMessaging(object : PushTokenHandler { override suspend fun onTokenRefresh(token: PushToken) { action(token) } }, dispatcher)
    private fun DefaultMessaging.attach(action: suspend () -> PushTokenResult) { attachSink(object : MessagingSink { override suspend fun getToken() = action() }) }
    @Test fun platformBootstrapValidatesCallbacksWithoutThrowingOrInvokingHandlerForInvalidTokens() = runTest {
        val delivered = mutableListOf<PushToken>()
        val core = core(StandardTestDispatcher(testScheduler)) { delivered += it }
        val bootstrap: MessagingBootstrap = core
        try {
            bootstrap.onRegistrationToken("white space")
            advanceUntilIdle()
            assertEquals(MessagingFailure.InvalidToken, (core.token.value as PushTokenResult.Failed).reason)
            assertTrue(delivered.isEmpty())
            bootstrap.onRegistrationToken("fixture")
            advanceUntilIdle()
            assertEquals(listOf(PushToken("fixture")), delivered)
            assertEquals(TokenSyncStatus.Synced, core.syncStatus.value)
            core.close()
            bootstrap.onRegistrationToken("after_close")
            assertEquals(TokenUnavailableReason.Closed, (core.token.value as PushTokenResult.Unavailable).reason)
            assertEquals(1, delivered.size)
        } finally { core.close() }
    }
    @Test fun tokenIsValidatedAndRedacted() {
        assertEquals("PushToken(redacted)", PushToken("secret").toString())
        assertFailsWith<IllegalArgumentException> { PushToken("") }
        assertFailsWith<IllegalArgumentException> { PushToken("white space") }
        assertFailsWith<IllegalArgumentException> { PushToken("x".repeat(4097)) }
    }
    @Test fun notInitializedAndPlatformNotReadyAreExplicit() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        assertEquals(TokenUnavailableReason.NotInitialized, (core.getToken() as PushTokenResult.Unavailable).reason)
        core.attach { PushTokenResult.Unavailable(TokenUnavailableReason.PlatformNotReady) }
        assertEquals(TokenUnavailableReason.PlatformNotReady, (core.getToken() as PushTokenResult.Unavailable).reason)
        core.close()
    }
    @Test fun sdkFailureIsNotConvertedToMissingToken() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); core.attach { error("SDK secret") }
        assertEquals(MessagingFailure.SdkFailure, (core.getToken() as PushTokenResult.Failed).reason); core.close()
    }
    @Test fun cancelledFetchPropagates() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); core.attach { awaitCancellation() }
        val job = launch { core.getToken(); fail("cancelled fetch returned") }; runCurrent(); job.cancelAndJoin(); core.close()
    }
    @Test fun refreshWinsAgainstOldFetch() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); val gate = CompletableDeferred<Unit>()
        core.attach { gate.await(); PushTokenResult.Available(PushToken("old")) }
        val result = async { core.getToken() }; runCurrent(); core.onTokenRefresh(PushToken("new")); gate.complete(Unit)
        assertEquals("new", (result.await() as PushTokenResult.Available).token.value); core.close()
    }
    @Test fun updatesConflateAndHandlerIsSerial() = runTest {
        val received = mutableListOf<String>(); val core = core(StandardTestDispatcher(testScheduler)) { received += it.value }
        repeat(100) { core.onTokenRefresh(PushToken("token_$it")) }; runCurrent()
        assertEquals(listOf("token_99"), received); assertEquals(TokenSyncStatus.Synced, core.syncStatus.value); core.close()
    }
    @Test fun handlerFailureIsObservableAndNextRefreshWorks() = runTest {
        var fail = true; val core = core(StandardTestDispatcher(testScheduler)) { if (fail) throw CancellationException("sync") }
        core.onTokenRefresh(PushToken("one")); runCurrent(); assertEquals(TokenSyncStatus.Failed, core.syncStatus.value)
        fail = false; core.onTokenRefresh(PushToken("two")); runCurrent(); assertEquals(TokenSyncStatus.Synced, core.syncStatus.value); core.close()
    }
    @Test fun repeatedSuccessfulTokenDoesNotLeavePendingStatus() = runTest {
        var calls = 0; val core = core(StandardTestDispatcher(testScheduler)) { calls++ }
        repeat(2) { core.onTokenRefresh(PushToken("same")); runCurrent() }
        assertEquals(1, calls); assertEquals(TokenSyncStatus.Synced, core.syncStatus.value); core.close()
    }
    @Test fun closeWinsAgainstInFlightFetch() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); val gate = CompletableDeferred<Unit>()
        core.attach { gate.await(); PushTokenResult.Available(PushToken("late")) }
        val result = async { core.getToken() }; runCurrent(); core.close(); gate.complete(Unit)
        assertEquals(TokenUnavailableReason.Closed, (result.await() as PushTokenResult.Unavailable).reason)
        core.onTokenRefresh(PushToken("ignored")); assertEquals(TokenSyncStatus.Closed, core.syncStatus.value)
    }
    @Test fun cancelledCallerStillClosesCore() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val job = launch { currentCoroutineContext().job.cancel(); core.close() }; job.join()
        assertEquals(TokenSyncStatus.Closed, core.syncStatus.value)
    }
    @Test fun tokenFetchDoesNotStrandCallbackSyncStatus() = runTest {
        val gate = CompletableDeferred<Unit>()
        val core = core(StandardTestDispatcher(testScheduler)) { gate.await() }
        core.attach { PushTokenResult.Available(PushToken("fetched")) }
        core.onTokenRefresh(PushToken("callback")); runCurrent()
        core.getToken(); gate.complete(Unit); runCurrent()
        assertEquals(TokenSyncStatus.Synced, core.syncStatus.value); core.close()
    }
    @Test fun invalidPlatformTokenIsExplicit() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); core.onRegistrationToken("bad token")
        assertEquals(MessagingFailure.InvalidToken, (core.token.value as PushTokenResult.Failed).reason)
        assertTrue(PushTokenDecoder.decode("bad token") is PushTokenResult.Failed); core.close()
    }
    @Test fun closeCancelsRunningHandlerAndPreventsFurtherCallbacks() = runTest {
        var cancelled = false; var calls = 0
        val core = core(StandardTestDispatcher(testScheduler)) {
            calls++
            try { awaitCancellation() } finally { cancelled = true }
        }
        core.onTokenRefresh(PushToken("first")); runCurrent(); core.close()
        core.onTokenRefresh(PushToken("after")); runCurrent()
        assertTrue(cancelled); assertEquals(1, calls)
    }
    @Test fun fetchesNeverOverlap() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); var active = 0; var peak = 0
        core.attach {
            active++; peak = maxOf(peak, active); yield(); active--
            PushTokenResult.Available(PushToken("one"))
        }
        List(4) { async { core.getToken() } }.awaitAll()
        assertEquals(1, peak); core.close()
    }

    @Test fun cancelledFetchCannotPublishLateNonCancellableSdkResult() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); val gate = CompletableDeferred<Unit>()
        core.attach {
            withContext(NonCancellable) { gate.await() }
            PushTokenResult.Available(PushToken("late"))
        }
        val job = launch { core.getToken() }; runCurrent(); job.cancel(); gate.complete(Unit); job.join()
        assertTrue(core.token.value is PushTokenResult.Unavailable); core.close()
    }

}
