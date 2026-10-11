package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.lifecycle.MessagingBootstrap
import com.grippo.toolkit.messaging.state.PushTokenFailure
import com.grippo.toolkit.messaging.state.PushTokenState
import com.grippo.toolkit.messaging.state.PushTokenUnavailableReason
import com.grippo.toolkit.messaging.synchronization.PushTokenSyncStatus
import com.grippo.toolkit.messaging.synchronization.PushTokenSynchronizer
import com.grippo.toolkit.messaging.token.PushToken
import com.grippo.toolkit.messaging.transport.PushTokenParser
import com.grippo.toolkit.messaging.transport.PushTokenProvider
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

class MessagingTest {
    @Test fun staleWakeupDoesNotRetryFailedCallbackButANewCallbackCanRetry() = runTest {
        var attempts = 0
        val core = core(UnconfinedTestDispatcher(testScheduler)) {
            attempts++
            error("Fixture synchronizer failure")
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            core.tokenState.collect { result ->
                if (result is PushTokenState.Available && result.token.value == "first") {
                    core.onTokenChanged("second")
                }
            }
        }
        try {
            core.onTokenChanged("first")
            runCurrent()
            assertEquals(1, attempts, "A wakeup from the older callback retried the latest failed callback")
            assertEquals(PushTokenSyncStatus.Failed, core.tokenSyncStatus.value)
            core.onTokenChanged("second")
            runCurrent()
            assertEquals(2, attempts)
        } finally { core.close() }
    }

    @Test fun reentrantTokenObserverCannotReplaceANewerCallbackWithAnOlderToken() = runTest {
        val delivered = mutableListOf<PushToken>()
        val core = core(StandardTestDispatcher(testScheduler)) { delivered += it }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            core.tokenState.collect { result ->
                if (result is PushTokenState.Available && result.token.value == "first") {
                    core.onTokenChanged("second")
                }
            }
        }
        try {
            core.onTokenChanged("first")
            advanceUntilIdle()
            assertEquals("second", (core.tokenState.value as PushTokenState.Available).token.value)
            assertEquals(listOf(PushToken("second")), delivered)
            assertEquals(PushTokenSyncStatus.Synced, core.tokenSyncStatus.value)
        } finally { core.close() }
    }

    private fun core(dispatcher: CoroutineDispatcher, action: suspend (PushToken) -> Unit = {}): DefaultMessaging =
        DefaultMessaging(object : PushTokenSynchronizer { override suspend fun syncToken(token: PushToken) { action(token) } }, dispatcher)
    private fun DefaultMessaging.attach(action: suspend () -> PushTokenState) { attachTokenProvider(object : PushTokenProvider { override suspend fun fetchToken() = action() }) }
    @Test fun platformBootstrapValidatesCallbacksWithoutThrowingOrInvokingSynchronizerForInvalidTokens() = runTest {
        val delivered = mutableListOf<PushToken>()
        val core = core(StandardTestDispatcher(testScheduler)) { delivered += it }
        val bootstrap: MessagingBootstrap = core
        try {
            bootstrap.onTokenChanged("white space")
            advanceUntilIdle()
            assertEquals(PushTokenFailure.InvalidToken, (core.tokenState.value as PushTokenState.Failed).reason)
            assertTrue(delivered.isEmpty())
            bootstrap.onTokenChanged("fixture")
            advanceUntilIdle()
            assertEquals(listOf(PushToken("fixture")), delivered)
            assertEquals(PushTokenSyncStatus.Synced, core.tokenSyncStatus.value)
            core.close()
            bootstrap.onTokenChanged("after_close")
            assertEquals(PushTokenUnavailableReason.Closed, (core.tokenState.value as PushTokenState.Unavailable).reason)
            assertEquals(1, delivered.size)
        } finally { core.close() }
    }
    @Test fun tokenIsValidatedAndRedacted() {
        assertEquals("PushToken(redacted)", PushToken("secret").toString())
        assertFailsWith<IllegalArgumentException> { PushToken("") }
        assertFailsWith<IllegalArgumentException> { PushToken("white space") }
        assertFailsWith<IllegalArgumentException> { PushToken("x".repeat(4097)) }
    }
    @Test fun providerNotAttachedAndPlatformNotReadyAreExplicit() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        assertEquals(PushTokenUnavailableReason.ProviderNotAttached, (core.fetchToken() as PushTokenState.Unavailable).reason)
        core.attach { PushTokenState.Unavailable(PushTokenUnavailableReason.PlatformNotReady) }
        assertEquals(PushTokenUnavailableReason.PlatformNotReady, (core.fetchToken() as PushTokenState.Unavailable).reason)
        core.close()
    }
    @Test fun providerFailureIsNotConvertedToMissingToken() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); core.attach { error("SDK secret") }
        assertEquals(PushTokenFailure.ProviderFailure, (core.fetchToken() as PushTokenState.Failed).reason); core.close()
    }
    @Test fun cancelledFetchPropagates() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); core.attach { awaitCancellation() }
        val job = launch { core.fetchToken(); fail("cancelled fetch returned") }; runCurrent(); job.cancelAndJoin(); core.close()
    }
    @Test fun refreshWinsAgainstOldFetch() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); val gate = CompletableDeferred<Unit>()
        core.attach { gate.await(); PushTokenState.Available(PushToken("old")) }
        val result = async { core.fetchToken() }; runCurrent(); core.acceptToken(PushToken("new")); gate.complete(Unit)
        assertEquals("new", (result.await() as PushTokenState.Available).token.value); core.close()
    }
    @Test fun updatesConflateAndSynchronizerIsSerial() = runTest {
        val received = mutableListOf<String>(); val core = core(StandardTestDispatcher(testScheduler)) { received += it.value }
        repeat(100) { core.acceptToken(PushToken("token_$it")) }; runCurrent()
        assertEquals(listOf("token_99"), received); assertEquals(PushTokenSyncStatus.Synced, core.tokenSyncStatus.value); core.close()
    }
    @Test fun synchronizerFailureIsObservableAndNextRefreshWorks() = runTest {
        var fail = true; val core = core(StandardTestDispatcher(testScheduler)) { if (fail) throw CancellationException("sync") }
        core.acceptToken(PushToken("one")); runCurrent(); assertEquals(PushTokenSyncStatus.Failed, core.tokenSyncStatus.value)
        fail = false; core.acceptToken(PushToken("two")); runCurrent(); assertEquals(PushTokenSyncStatus.Synced, core.tokenSyncStatus.value); core.close()
    }
    @Test fun repeatedSuccessfulTokenDoesNotLeavePendingStatus() = runTest {
        var calls = 0; val core = core(StandardTestDispatcher(testScheduler)) { calls++ }
        repeat(2) { core.acceptToken(PushToken("same")); runCurrent() }
        assertEquals(1, calls); assertEquals(PushTokenSyncStatus.Synced, core.tokenSyncStatus.value); core.close()
    }
    @Test fun closeWinsAgainstInFlightFetch() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); val gate = CompletableDeferred<Unit>()
        core.attach { gate.await(); PushTokenState.Available(PushToken("late")) }
        val result = async { core.fetchToken() }; runCurrent(); core.close(); gate.complete(Unit)
        assertEquals(PushTokenUnavailableReason.Closed, (result.await() as PushTokenState.Unavailable).reason)
        core.acceptToken(PushToken("ignored")); assertEquals(PushTokenSyncStatus.Closed, core.tokenSyncStatus.value)
    }
    @Test fun cancelledCallerStillClosesCore() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val job = launch { currentCoroutineContext().job.cancel(); core.close() }; job.join()
        assertEquals(PushTokenSyncStatus.Closed, core.tokenSyncStatus.value)
    }
    @Test fun tokenFetchDoesNotStrandCallbackSyncStatus() = runTest {
        val gate = CompletableDeferred<Unit>()
        val core = core(StandardTestDispatcher(testScheduler)) { gate.await() }
        core.attach { PushTokenState.Available(PushToken("fetched")) }
        core.acceptToken(PushToken("callback")); runCurrent()
        core.fetchToken(); gate.complete(Unit); runCurrent()
        assertEquals(PushTokenSyncStatus.Synced, core.tokenSyncStatus.value); core.close()
    }
    @Test fun invalidPlatformTokenIsExplicit() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); core.onTokenChanged("bad token")
        assertEquals(PushTokenFailure.InvalidToken, (core.tokenState.value as PushTokenState.Failed).reason)
        assertTrue(PushTokenParser.parse("bad token") is PushTokenState.Failed); core.close()
    }
    @Test fun closeCancelsRunningSynchronizerAndPreventsFurtherCallbacks() = runTest {
        var cancelled = false; var calls = 0
        val core = core(StandardTestDispatcher(testScheduler)) {
            calls++
            try { awaitCancellation() } finally { cancelled = true }
        }
        core.acceptToken(PushToken("first")); runCurrent(); core.close()
        core.acceptToken(PushToken("after")); runCurrent()
        assertTrue(cancelled); assertEquals(1, calls)
    }
    @Test fun fetchesNeverOverlap() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); var active = 0; var peak = 0
        core.attach {
            active++; peak = maxOf(peak, active); yield(); active--
            PushTokenState.Available(PushToken("one"))
        }
        List(4) { async { core.fetchToken() } }.awaitAll()
        assertEquals(1, peak); core.close()
    }

    @Test fun cancelledFetchCannotPublishLateNonCancellableSdkResult() = runTest {
        val core = core(StandardTestDispatcher(testScheduler)); val gate = CompletableDeferred<Unit>()
        core.attach {
            withContext(NonCancellable) { gate.await() }
            PushTokenState.Available(PushToken("late"))
        }
        val job = launch { core.fetchToken() }; runCurrent(); job.cancel(); gate.complete(Unit); job.join()
        assertTrue(core.tokenState.value is PushTokenState.Unavailable); core.close()
    }

}
