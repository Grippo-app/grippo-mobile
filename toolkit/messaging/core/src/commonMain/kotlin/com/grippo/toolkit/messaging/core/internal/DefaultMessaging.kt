package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.Messaging
import com.grippo.toolkit.messaging.lifecycle.MessagingBootstrap
import com.grippo.toolkit.messaging.state.PushTokenFailure
import com.grippo.toolkit.messaging.state.PushTokenState
import com.grippo.toolkit.messaging.state.PushTokenUnavailableReason
import com.grippo.toolkit.messaging.synchronization.PushTokenSyncStatus
import com.grippo.toolkit.messaging.synchronization.PushTokenSynchronizer
import com.grippo.toolkit.messaging.token.PushToken
import com.grippo.toolkit.messaging.transport.PushTokenProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

internal class DefaultMessaging(
    private val synchronizer: PushTokenSynchronizer,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Messaging, MessagingBootstrap {
    private val lock = MessagingLock()
    private val fetchMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var latestTokenState: PushTokenState =
        PushTokenState.Unavailable(PushTokenUnavailableReason.ProviderNotAttached)
    private var latestSyncStatus = PushTokenSyncStatus.Idle
    private val publishedTokenState = MutableStateFlow(latestTokenState)
    private val publishedSyncStatus = MutableStateFlow(latestSyncStatus)
    override val tokenState: StateFlow<PushTokenState> = publishedTokenState.asStateFlow()
    override val tokenSyncStatus: StateFlow<PushTokenSyncStatus> = publishedSyncStatus.asStateFlow()
    private var provider: PushTokenProvider? = null
    private var closed = false
    private var revision = 0L

    private var syncRequest: PushTokenState.Available? = null

    init {
        scope.launch {
            var delivered: PushToken? = null
            var attempted: PushTokenState.Available? = null
            for (ignored in wake) {
                yield()
                val request = lock.withLock { if (closed) null else syncRequest } ?: continue
                if (request === attempted) continue
                attempted = request
                val next = request.token
                if (next == delivered) {
                    completeSync(request, PushTokenSyncStatus.Synced)
                    continue
                }
                try {
                    synchronizer.syncToken(next)
                    delivered = next
                    completeSync(request, PushTokenSyncStatus.Synced)
                } catch (_: Exception) {
                    currentCoroutineContext().ensureActive()
                    completeSync(request, PushTokenSyncStatus.Failed)
                }
            }
        }
    }

    override fun attachTokenProvider(tokenProvider: PushTokenProvider): Unit = lock.withLock {
        check(!closed && this.provider == null)
        this.provider = tokenProvider
    }

    override fun onTokenChanged(rawToken: String) {
        val token = try {
            PushToken(rawToken)
        } catch (_: IllegalArgumentException) {
            onInvalidToken()
            return
        }
        acceptToken(token)
    }

    private fun acceptToken(token: PushToken) {
        val changed = lock.withLock {
            if (closed) return@withLock false
            revision++
            val request = PushTokenState.Available(token)
            syncRequest = request
            latestTokenState = request
            latestSyncStatus = PushTokenSyncStatus.Pending
            true
        }
        if (changed) {
            publishToken()
            publishSyncStatus()
            wake.trySend(Unit)
        }
    }

    private fun onInvalidToken() {
        val changed = lock.withLock {
            if (closed) return@withLock false
            revision++
            latestTokenState = PushTokenState.Failed(PushTokenFailure.InvalidToken)
            true
        }
        if (changed) publishToken()
    }

    override suspend fun fetchToken(): PushTokenState = fetchMutex.withLock {
        val snapshot = lock.withLock { FetchSnapshot(closed, provider, revision) }
        if (snapshot.closed) return@withLock PushTokenState.Unavailable(PushTokenUnavailableReason.Closed)
        val target = snapshot.provider ?: return@withLock PushTokenState.Unavailable(
            PushTokenUnavailableReason.ProviderNotAttached
        )
        val result = target.fetchToken()
        currentCoroutineContext().ensureActive()
        lock.withLock {
            if (!closed && revision == snapshot.revision) {
                latestTokenState = result
                revision++
            }
        }
        publishToken()
        lock.withLock { latestTokenState }
    }

    private class FetchSnapshot(val closed: Boolean, val provider: PushTokenProvider?, val revision: Long)

    private fun completeSync(request: PushTokenState.Available, value: PushTokenSyncStatus) {
        val changed = lock.withLock {
            if (closed || syncRequest !== request) return@withLock false
            latestSyncStatus = value
            true
        }
        if (changed) publishSyncStatus()
    }

    // StateFlow may resume observers inline: publish outside the lock and reconcile canonical state.
    private fun publishToken() {
        publishState(publishedTokenState) { lock.withLock { latestTokenState } }
    }

    private fun publishSyncStatus() {
        publishState(publishedSyncStatus) { lock.withLock { latestSyncStatus } }
    }

    suspend fun close() {
        lock.withLock {
            closed = true
            revision++
            provider = null
            syncRequest = null
            latestTokenState = PushTokenState.Unavailable(PushTokenUnavailableReason.Closed)
            latestSyncStatus = PushTokenSyncStatus.Closed
        }
        publishToken()
        publishSyncStatus()
        withContext(NonCancellable) {
            scope.coroutineContext.job.cancelAndJoin()
            wake.cancel()
        }
    }
}
