package com.grippo.toolkit.crashreporting.core.internal

import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.lifecycle.CrashReportingBootstrap
import com.grippo.toolkit.crashreporting.transport.CrashReportingSink
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** SDK owns fatal capture/persistence. One background worker owns all nonfatal/log submissions. */
internal class DefaultCrashReporting(
    private val capacity: Int = CrashReportingQueuePolicy.DEFAULT_CAPACITY,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CrashReporting, CrashReportingBootstrap {
    private val lock = CrashReportingLock()
    private val queue = ArrayDeque<Work>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var sink: CrashReportingSink? = null
    private var closed = false
    private val counts = LongArray(CrashReportingDisposition.entries.size)

    init {
        require(capacity in 1..CrashReportingQueuePolicy.MAXIMUM_CAPACITY)
        scope.launch {
            for (ignored in wake) {
                var processed = 0
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val next = lock.withLock {
                        val target = sink
                        if (closed || target == null || queue.isEmpty()) null
                        else Submission(target, queue.removeFirst())
                    } ?: break
                    submit(next)
                    if (++processed == CrashReportingQueuePolicy.SUBMISSIONS_PER_BATCH) {
                        processed = 0; yield()
                    }
                }
            }
        }
    }

    override fun attachSink(crashSink: CrashReportingSink) {
        lock.withLock {
            check(!closed && this.sink == null)
            this.sink = crashSink
        }
        wake.trySend(Unit)
    }

    override fun reportException(throwable: Throwable, context: CrashReportContext) {
        if (throwable is CancellationException) {
            lock.withLock { increment(CrashReportingDisposition.CancellationIgnored) }
            return
        }
        enqueue(Work.Report(throwable, context))
    }

    override fun logBreadcrumb(code: CrashDiagnosticCode) {
        enqueue(Work.Breadcrumb(code))
    }

    fun count(disposition: CrashReportingDisposition): Long =
        lock.withLock { counts[disposition.ordinal] }

    suspend fun close() {
        lock.withLock {
            if (!closed) {
                closed = true
                counts[CrashReportingDisposition.Closed.ordinal] += queue.size
                queue.clear()
                sink = null
            }
        }
        withContext(NonCancellable) {
            scope.coroutineContext.job.cancelAndJoin()
            wake.cancel()
        }
    }

    private fun enqueue(work: Work) {
        val queued = lock.withLock {
            when {
                closed -> {
                    increment(CrashReportingDisposition.Closed); false
                }

                queue.size == capacity -> {
                    increment(CrashReportingDisposition.Overflow); false
                }

                else -> {
                    queue.addLast(work); increment(CrashReportingDisposition.Queued); true
                }
            }
        }
        // Signal outside the state lock because a dispatcher may resume the worker immediately.
        if (queued) wake.trySend(Unit)
    }

    private suspend fun submit(submission: Submission) {
        try {
            currentCoroutineContext().ensureActive()
            when (val work = submission.work) {
                is Work.Report -> submission.sink.reportException(work.throwable, work.context)
                is Work.Breadcrumb -> submission.sink.logBreadcrumb(work.code)
            }
            lock.withLock { increment(CrashReportingDisposition.Submitted) }
        } catch (_: Exception) {
            val context = currentCoroutineContext()
            lock.withLock {
                increment(if (context.isActive) CrashReportingDisposition.TransportFailure else CrashReportingDisposition.Closed)
            }
            context.ensureActive()
        }
    }

    private fun increment(value: CrashReportingDisposition) {
        counts[value.ordinal]++
    }

    private class Submission(val sink: CrashReportingSink, val work: Work)
    private sealed interface Work {
        class Report(val throwable: Throwable, val context: CrashReportContext) : Work
        class Breadcrumb(val code: CrashDiagnosticCode) : Work
    }
}
