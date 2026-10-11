package com.grippo.toolkit.analytics.core

import com.grippo.toolkit.analytics.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** One worker owns SDK calls; the lock only protects in-memory state, never transport work. */
public class DefaultAnalytics(
    private val catalog: AnalyticsCatalog = AnalyticsCatalog(),
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val queueCapacity: Int = 128,
    private val diagnosticCapacity: Int = 0,
    private val validationMode: AnalyticsValidationMode = AnalyticsValidationMode.Reject,
) : Analytics {
    private val lock = AnalyticsLock()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val queue = ArrayDeque<AnalyticsPayload>()
    private val diagnostics = ArrayDeque<AnalyticsDiagnostic>()
    private var context = AnalyticsContext()
    private var identity = AnalyticsIdentity()
    private var collection = AnalyticsCollection.Unknown
    private var sink: AnalyticsSink? = null
    private var revision = 0L
    private var appliedRevision = -1L
    private var failedRevision: Long? = null
    private var closed = false

    init {
        require(queueCapacity > 0 && diagnosticCapacity >= 0)
        scope.launch {
            for (ignored in wake) {
                while (true) {
                    val work = lock.withLock { nextWork() } ?: break
                    when (work) {
                        is Work.Configure -> configure(work)
                        is Work.Send -> send(work)
                    }
                }
            }
        }
    }

    /** Attach exactly once after platform SDK initialization. Pre-ready permitted events are bounded. */
    public fun attachSink(sink: AnalyticsSink): Unit = lock.withLock {
        check(!closed && this.sink == null) { "Analytics sink already attached or closed" }
        this.sink = sink
        revision++
        wake.trySend(Unit)
    }

    override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?): Unit = lock.withLock {
        if (closed) { record(AnalyticsDisposition.Closed); return@withLock }
        if (collection != AnalyticsCollection.Granted) {
            record(AnalyticsDisposition.CollectionDisabled)
            return@withLock
        }
        if (failedRevision == revision) {
            record(AnalyticsDisposition.TransportFailure)
            return@withLock
        }
        val payload = try {
            catalog.encode(event, context, flow, operation)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            record(AnalyticsDisposition.InvalidEvent)
            if (validationMode == AnalyticsValidationMode.Throw) throw IllegalArgumentException("Invalid analytics event", e)
            return@withLock
        }
        if (queue.size == queueCapacity) {
            record(AnalyticsDisposition.QueueFull, payload.name)
            return@withLock
        }
        queue.addLast(payload)
        record(AnalyticsDisposition.Queued, payload.name)
        wake.trySend(Unit)
    }

    override fun updateContext(context: AnalyticsContext): Unit = lock.withLock {
        if (!closed) this.context = context
    }

    override fun updateIdentity(identity: AnalyticsIdentity): Unit = lock.withLock {
        if (closed) return@withLock
        try { catalog.validateIdentity(identity) } catch (e: IllegalArgumentException) {
            record(AnalyticsDisposition.InvalidIdentity)
            if (validationMode == AnalyticsValidationMode.Throw) throw e
            return@withLock
        }
        if (this.identity.userId == identity.userId &&
            this.identity.properties.map { it.key to it.value } == identity.properties.map { it.key to it.value }) return@withLock
        discardQueue(AnalyticsDisposition.DiscardedOnIdentityChange)
        this.identity = identity
        revision++
        wake.trySend(Unit)
    }

    override fun updateCollection(collection: AnalyticsCollection): Unit = lock.withLock {
        if (closed || this.collection == collection) return@withLock
        discardQueue(AnalyticsDisposition.DiscardedOnPolicyChange)
        this.collection = collection
        revision++
        wake.trySend(Unit)
    }

    /** Retry failed SDK configuration explicitly; no automatic loop or event replay. */
    public fun retrySinkConfiguration(): Unit = lock.withLock {
        if (!closed && sink != null) {
            revision++
            wake.trySend(Unit)
        }
    }

    public fun diagnostics(): List<AnalyticsDiagnostic> = lock.withLock { diagnostics.toList() }
    public fun queuedEventCount(): Int = lock.withLock { queue.size }

    /** App-lifetime service normally remains open. Tests/tools can shut down deterministically. */
    public suspend fun close() {
        val target = lock.withLock {
            if (closed) return@withLock null
            closed = true
            queue.clear()
            sink
        }
        scope.coroutineContext[Job]?.cancelAndJoin()
        wake.close()
        try { target?.setCollection(AnalyticsCollection.Denied) } catch (e: Exception) {
            if (e is CancellationException) throw e
            lock.withLock { record(AnalyticsDisposition.TransportFailure) }
        }
    }

    private fun nextWork(): Work? {
        if (closed) return null
        val target = sink ?: return null
        if (appliedRevision != revision) return Work.Configure(target, revision, collection, identity)
        if (collection != AnalyticsCollection.Granted || failedRevision == revision || queue.isEmpty()) return null
        return Work.Send(target, revision, queue.removeFirst())
    }

    private fun configure(work: Work.Configure) {
        val succeeded = try {
            // A temporary denial here would revoke consent and reset valid SDK data on every identity update.
            if (work.collection != AnalyticsCollection.Granted) work.sink.setCollection(work.collection)
            work.sink.setIdentity(catalog.encodeIdentity(if (work.collection == AnalyticsCollection.Granted) work.identity else AnalyticsIdentity()))
            val current = lock.withLock { !closed && revision == work.revision }
            if (current && work.collection == AnalyticsCollection.Granted) work.sink.setCollection(work.collection)
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            try { work.sink.setCollection(AnalyticsCollection.Denied) } catch (disableError: Exception) {
                if (disableError is CancellationException) throw disableError
            }
            lock.withLock { record(AnalyticsDisposition.TransportFailure) }
            false
        }
        lock.withLock {
            appliedRevision = work.revision
            failedRevision = if (succeeded) null else work.revision
            if (!succeeded) queue.clear()
        }
    }

    private fun send(work: Work.Send) {
        // Revocation invalidates queued work. An SDK call already in flight cannot be retracted.
        if (!lock.withLock { !closed && revision == work.revision && collection == AnalyticsCollection.Granted }) return
        try {
            work.sink.send(work.payload)
            lock.withLock { record(AnalyticsDisposition.AcceptedBySink, work.payload.name) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            lock.withLock { record(AnalyticsDisposition.TransportFailure, work.payload.name) }
        }
    }

    private fun discardQueue(reason: AnalyticsDisposition) {
        queue.forEach { record(reason, it.name) }
        queue.clear()
    }

    private fun record(disposition: AnalyticsDisposition, name: String? = null) {
        if (diagnosticCapacity == 0) return
        if (diagnostics.size == diagnosticCapacity) diagnostics.removeFirst()
        diagnostics.addLast(AnalyticsDiagnostic(disposition, name))
    }

    private sealed interface Work {
        data class Configure(val sink: AnalyticsSink, val revision: Long,
            val collection: AnalyticsCollection, val identity: AnalyticsIdentity) : Work
        data class Send(val sink: AnalyticsSink, val revision: Long, val payload: AnalyticsPayload) : Work
    }
}
