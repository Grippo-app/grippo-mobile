package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.Messaging
import com.grippo.toolkit.messaging.MessagingBootstrap
import com.grippo.toolkit.messaging.MessagingFailure
import com.grippo.toolkit.messaging.MessagingSink
import com.grippo.toolkit.messaging.PushToken
import com.grippo.toolkit.messaging.PushTokenHandler
import com.grippo.toolkit.messaging.PushTokenResult
import com.grippo.toolkit.messaging.TokenSyncStatus
import com.grippo.toolkit.messaging.TokenUnavailableReason
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

/** Latest-token-wins queue, one serial handler, bounded memory and no implicit retries. */
internal class DefaultMessaging(
    private val handler: PushTokenHandler,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Messaging, MessagingBootstrap {
    private val lock = MessagingLock()
    private val fetch = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var tokenState: PushTokenResult =
        PushTokenResult.Unavailable(TokenUnavailableReason.NotInitialized)
    private var syncState = TokenSyncStatus.Idle
    private val current = MutableStateFlow(tokenState)
    private val status = MutableStateFlow(syncState)
    override val token: StateFlow<PushTokenResult> = current.asStateFlow()
    override val syncStatus: StateFlow<TokenSyncStatus> = status.asStateFlow()
    private var sink: MessagingSink? = null
    private var closed = false
    private var revision = 0L

    // Reuse the published value as callback identity; SDK fetches never create sync requests.
    private var syncRequest: PushTokenResult.Available? = null

    init {
        scope.launch {
            var delivered: PushToken? = null
            var attempted: PushTokenResult.Available? = null
            for (ignored in wake) {
                currentCoroutineContext().ensureActive()
                yield()
                val request = lock.withLock { if (closed) null else syncRequest } ?: continue
                if (request === attempted) continue
                attempted = request
                val next = request.token
                if (next == delivered) {
                    completeSync(request, TokenSyncStatus.Synced)
                    continue
                }
                try {
                    handler.onTokenRefresh(next)
                    delivered = next
                    completeSync(request, TokenSyncStatus.Synced)
                } catch (_: Exception) {
                    currentCoroutineContext().ensureActive()
                    completeSync(request, TokenSyncStatus.Failed)
                }
            }
        }
    }

    override fun attachSink(messagingSink: MessagingSink): Unit = lock.withLock {
        check(!closed && this.sink == null)
        this.sink = messagingSink
    }

    override fun onRegistrationToken(value: String) {
        val token = try {
            PushToken(value)
        } catch (_: IllegalArgumentException) {
            onInvalidToken()
            return
        }
        onTokenRefresh(token)
    }

    internal fun onTokenRefresh(token: PushToken) {
        val changed = lock.withLock {
            if (closed) return@withLock false
            revision++
            val request = PushTokenResult.Available(token)
            syncRequest = request
            tokenState = request
            syncState = TokenSyncStatus.Pending
            true
        }
        if (changed) {
            publishToken()
            publishSyncStatus()
            // Signals carry no token; reordered wakeups always read the latest request.
            wake.trySend(Unit)
        }
    }

    private fun onInvalidToken() {
        val changed = lock.withLock {
            if (closed) return@withLock false
            revision++
            tokenState = PushTokenResult.Failed(MessagingFailure.InvalidToken)
            true
        }
        if (changed) publishToken()
    }

    override suspend fun getToken(): PushTokenResult = fetch.withLock {
        val snapshot = lock.withLock { FetchSnapshot(closed, sink, revision) }
        if (snapshot.closed) return@withLock PushTokenResult.Unavailable(TokenUnavailableReason.Closed)
        val target = snapshot.sink ?: return@withLock PushTokenResult.Unavailable(
            TokenUnavailableReason.NotInitialized
        )
        val result = try {
            target.getToken()
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            PushTokenResult.Failed(MessagingFailure.SdkFailure)
        }
        currentCoroutineContext().ensureActive()
        lock.withLock {
            if (!closed && revision == snapshot.revision) {
                tokenState = result; revision++
            }
        }
        publishToken()
        lock.withLock { tokenState }
    }

    private class FetchSnapshot(val closed: Boolean, val sink: MessagingSink?, val revision: Long)

    private fun completeSync(request: PushTokenResult.Available, value: TokenSyncStatus) {
        val changed = lock.withLock {
            if (closed || syncRequest !== request) return@withLock false
            syncState = value
            true
        }
        if (changed) publishSyncStatus()
    }

    // StateFlow may resume observers inline: publish outside the lock and reconcile canonical state.
    private fun publishToken() {
        publishState(current) { lock.withLock { tokenState } }
    }

    private fun publishSyncStatus() {
        publishState(status) { lock.withLock { syncState } }
    }

    suspend fun close() {
        lock.withLock {
            closed = true
            revision++
            sink = null
            syncRequest = null
            tokenState = PushTokenResult.Unavailable(TokenUnavailableReason.Closed)
            syncState = TokenSyncStatus.Closed
        }
        publishToken()
        publishSyncStatus()
        withContext(NonCancellable) {
            scope.coroutineContext.job.cancelAndJoin()
            wake.cancel()
        }
    }
}
