package com.grippo.toolkit.crashreporting.core

import com.grippo.toolkit.crashreporting.CrashCode
import com.grippo.toolkit.crashreporting.CrashContext
import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.CrashReportingBootstrap
import com.grippo.toolkit.crashreporting.CrashSink
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

internal expect class ServiceLock() {
    fun <T> withLock(block: () -> T): T
}

public enum class CrashDisposition {
    Submitted,
    Queued,
    Overflow,
    TransportFailure,
    CancellationIgnored,
    Closed
}

/** SDK owns fatal capture/persistence. One background worker owns all nonfatal/log submissions. */
public class DefaultCrashReporting(
    private val capacity: Int = 16,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CrashReporting, CrashReportingBootstrap {
    private val lock = ServiceLock()
    private val queue = ArrayDeque<Work>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var sink: CrashSink? = null
    private var closed = false
    private val counts = LongArray(CrashDisposition.entries.size)

    init {
        require(capacity in 1..128)
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
                    if (++processed == 32) {
                        processed = 0; yield()
                    }
                }
            }
        }
    }

    override fun attachSink(crashSink: CrashSink) {
        lock.withLock {
            check(!closed && this.sink == null)
            this.sink = crashSink
        }
        wake.trySend(Unit)
    }

    override fun record(throwable: Throwable, context: CrashContext) {
        if (throwable is CancellationException) {
            lock.withLock { increment(CrashDisposition.CancellationIgnored) }
            return
        }
        enqueue(Work.Report(throwable, context))
    }

    override fun breadcrumb(code: CrashCode) {
        enqueue(Work.Breadcrumb(code))
    }

    public fun count(disposition: CrashDisposition): Long =
        lock.withLock { counts[disposition.ordinal] }

    public suspend fun close() {
        lock.withLock {
            if (!closed) {
                closed = true
                counts[CrashDisposition.Closed.ordinal] += queue.size
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
                    increment(CrashDisposition.Closed); false
                }

                queue.size == capacity -> {
                    increment(CrashDisposition.Overflow); false
                }

                else -> {
                    queue.addLast(work); increment(CrashDisposition.Queued); true
                }
            }
        }
        // Outside the state lock, including when tests inject an immediate dispatcher.
        if (queued) wake.trySend(Unit)
    }

    private suspend fun submit(submission: Submission) {
        try {
            currentCoroutineContext().ensureActive()
            when (val work = submission.work) {
                is Work.Report -> submission.sink.record(work.throwable, work.context)
                is Work.Breadcrumb -> submission.sink.breadcrumb(work.code)
            }
            lock.withLock { increment(CrashDisposition.Submitted) }
        } catch (_: Exception) {
            val context = currentCoroutineContext()
            lock.withLock {
                increment(if (context.isActive) CrashDisposition.TransportFailure else CrashDisposition.Closed)
            }
            context.ensureActive()
        }
    }

    private fun increment(value: CrashDisposition) {
        counts[value.ordinal]++
    }

    private class Submission(val sink: CrashSink, val work: Work)
    private sealed interface Work {
        class Report(val throwable: Throwable, val context: CrashContext) : Work
        class Breadcrumb(val code: CrashCode) : Work
    }
}
