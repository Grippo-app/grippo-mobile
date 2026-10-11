package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.events.ScreenViewed
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.lifecycle.AnalyticsBootstrap
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.transport.AnalyticsIdentityPayload
import com.grippo.toolkit.analytics.transport.AnalyticsPayload
import com.grippo.toolkit.analytics.transport.AnalyticsSink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** One worker owns SDK calls; the lock only protects in-memory state, never transport work. */
internal class DefaultAnalytics(
    private val catalog: AnalyticsCatalog = AnalyticsCatalog(),
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val queueCapacity: Int = 128,
    private val diagnosticCapacity: Int = 0,
    private val validationMode: AnalyticsValidationMode = AnalyticsValidationMode.Reject,
) : Analytics, AnalyticsBootstrap {
    private val lock = AnalyticsLock()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val queue = ArrayDeque<AnalyticsPayload>()
    private val diagnostics = ArrayDeque<AnalyticsDiagnostic>()
    private var context = AnalyticsContext()
    private var presentationRevision = 0L
    private val anonymousIdentity = catalog.encodeIdentity(AnalyticsIdentity())
    private var identity = anonymousIdentity
    private var collection = AnalyticsCollection.Unknown
    private var sink: AnalyticsSink? = null
    private var revision = 0L
    private var appliedRevision = -1L
    private var failedRevision: Long? = null
    private var closed = false

    init {
        require(queueCapacity > 0 && diagnosticCapacity >= 0)
        scope.launch {
            for (ignored: Unit in wake) {
                var processed = 0
                while (true) {
                    ensureActive()
                    val work = lock.withLock { nextWork() } ?: break
                    when (work) {
                        is Work.Configure -> configure(work)
                        is Work.Send -> send(work)
                    }
                    if (++processed == MAX_WORK_PER_TURN) {
                        processed = 0
                        yield()
                    }
                }
            }
        }
    }

    /** Attach exactly once after platform SDK initialization. Pre-ready permitted events are bounded. */
    override fun attachSink(analyticsSink: AnalyticsSink) {
        lock.withLock {
            check(!closed && sink == null) { "Analytics sink already attached or closed" }
            sink = analyticsSink
            revision++
        }
        wake.trySend(Unit)
    }

    override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
        val queued = lock.withLock { enqueue(event, flow, operation) }
        if (queued) wake.trySend(Unit)
    }

    override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation {
        val presentation = lock.withLock {
            presentationRevision++
            if (!closed) context = context.copy(navigation = navigation)
            Presentation(presentationRevision, enqueue(ScreenViewed(navigation.screen), flow, null))
        }
        if (presentation.queued) wake.trySend(Unit)
        return presentation
    }

    /** Caller holds the state lock; signal the worker only after releasing it. */
    private fun enqueue(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?): Boolean {
        if (closed) {
            record(AnalyticsDisposition.Closed); return false
        }
        if (collection != AnalyticsCollection.Granted) {
            record(AnalyticsDisposition.CollectionDisabled)
            return false
        }
        if (failedRevision == revision) {
            record(AnalyticsDisposition.TransportFailure)
            return false
        }
        val payload = try {
            catalog.encode(event, context, flow, operation)
        } catch (e: Exception) {
            record(AnalyticsDisposition.InvalidEvent)
            if (validationMode == AnalyticsValidationMode.Throw) throw IllegalArgumentException(
                "Invalid analytics event",
                e
            )
            return false
        }
        if (queue.size == queueCapacity) {
            record(AnalyticsDisposition.QueueFull, payload.name)
            return false
        }
        queue.addLast(payload)
        record(AnalyticsDisposition.Queued, payload.name)
        return true
    }

    override fun updateContext(context: AnalyticsContext): Unit = lock.withLock {
        if (!closed) {
            presentationRevision++
            this.context = context
        }
    }

    private inner class Presentation(private val revision: Long, val queued: Boolean) : ScreenPresentation {
        override fun close(): Unit = lock.withLock {
            if (!closed && revision == presentationRevision) {
                context = context.copy(navigation = null)
                presentationRevision++
            }
        }
    }

    override fun updateIdentity(identity: AnalyticsIdentity) {
        val changed = lock.withLock {
            if (closed) return@withLock false
            val next = try {
                catalog.encodeIdentity(identity)
            } catch (e: IllegalArgumentException) {
                record(AnalyticsDisposition.InvalidIdentity)
                if (validationMode == AnalyticsValidationMode.Throw) throw e
                return@withLock false
            }
            if (this.identity.userId == next.userId && this.identity.properties == next.properties) return@withLock false
            discardQueue(AnalyticsDisposition.DiscardedOnIdentityChange)
            this.identity = next
            revision++
            true
        }
        if (changed) wake.trySend(Unit)
    }

    override fun updateCollection(collection: AnalyticsCollection) {
        val changed = lock.withLock {
            if (closed || this.collection == collection) return@withLock false
            discardQueue(AnalyticsDisposition.DiscardedOnPolicyChange)
            this.collection = collection
            revision++
            true
        }
        if (changed) wake.trySend(Unit)
    }

    /** Retry failed SDK configuration explicitly; no automatic loop or event replay. */
    fun retrySinkConfiguration() {
        val changed = lock.withLock {
            if (closed || sink == null) return@withLock false
            revision++
            true
        }
        if (changed) wake.trySend(Unit)
    }

    fun diagnostics(): List<AnalyticsDiagnostic> = lock.withLock { diagnostics.toList() }
    fun queuedEventCount(): Int = lock.withLock { queue.size }

    /** App-lifetime service normally remains open. Tests/tools can shut down deterministically. */
    suspend fun close() {
        val target = lock.withLock {
            if (closed) return@withLock null
            closed = true
            discardQueue(AnalyticsDisposition.Closed)
            val attached = sink
            sink = null
            identity = anonymousIdentity
            context = AnalyticsContext()
            attached
        }
        withContext(NonCancellable) {
            scope.coroutineContext.job.cancelAndJoin()
            wake.cancel()
            try {
                target?.setCollection(AnalyticsCollection.Denied)
            } catch (_: Exception) {
                lock.withLock { record(AnalyticsDisposition.TransportFailure) }
            }
        }
    }

    private fun nextWork(): Work? {
        if (closed) return null
        val target = sink ?: return null
        if (appliedRevision != revision) return Work.Configure(
            target,
            revision,
            collection,
            identity
        )
        if (collection != AnalyticsCollection.Granted || failedRevision == revision || queue.isEmpty()) return null
        return Work.Send(target, revision, queue.removeFirst())
    }

    private suspend fun configure(work: Work.Configure) {
        val succeeded = try {
            if (work.collection != AnalyticsCollection.Granted) {
                work.sink.setCollection(work.collection)
                work.sink.setIdentity(anonymousIdentity)
            } else if (isCurrentRevision(work.revision)) {
                // Disabled SDKs may ignore identity writes. No product event is sent before both calls finish.
                // Never temporarily deny consent here: that can reset valid SDK data on identity updates.
                work.sink.setCollection(work.collection)
                if (isCurrentRevision(work.revision)) work.sink.setIdentity(work.identity)
            }
            true
        } catch (_: Exception) {
            // A synchronous sink exception is a transport failure unless this worker was cancelled.
            currentCoroutineContext().ensureActive()
            try {
                work.sink.setCollection(AnalyticsCollection.Denied)
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
            }
            lock.withLock { record(AnalyticsDisposition.TransportFailure) }
            false
        }
        lock.withLock {
            appliedRevision = work.revision
            failedRevision = if (succeeded) null else work.revision
            if (!succeeded && revision == work.revision) discardQueue(AnalyticsDisposition.TransportFailure)
        }
    }

    private fun isCurrentRevision(expected: Long): Boolean =
        lock.withLock { !closed && revision == expected }

    private suspend fun send(work: Work.Send) {
        // Revocation invalidates queued work. An SDK call already in flight cannot be retracted.
        val permitted = lock.withLock {
            val current =
                !closed && revision == work.revision && collection == AnalyticsCollection.Granted
            if (!current) record(AnalyticsDisposition.DiscardedBeforeSend, work.payload.name)
            current
        }
        if (!permitted) return
        try {
            work.sink.send(work.payload)
            lock.withLock { record(AnalyticsDisposition.AcceptedBySink, work.payload.name) }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            lock.withLock { record(AnalyticsDisposition.TransportFailure, work.payload.name) }
        }
    }

    private fun discardQueue(reason: AnalyticsDisposition) {
        if (diagnosticCapacity > 0) queue.forEach { record(reason, it.name) }
        queue.clear()
    }

    private fun record(disposition: AnalyticsDisposition, name: String? = null) {
        if (diagnosticCapacity == 0) return
        if (diagnostics.size == diagnosticCapacity) diagnostics.removeFirst()
        diagnostics.addLast(AnalyticsDiagnostic(disposition, name))
    }

    private sealed interface Work {
        data class Configure(
            val sink: AnalyticsSink, val revision: Long,
            val collection: AnalyticsCollection, val identity: AnalyticsIdentityPayload
        ) : Work

        data class Send(
            val sink: AnalyticsSink,
            val revision: Long,
            val payload: AnalyticsPayload
        ) : Work
    }

    private companion object {
        const val MAX_WORK_PER_TURN = 32
    }
}
