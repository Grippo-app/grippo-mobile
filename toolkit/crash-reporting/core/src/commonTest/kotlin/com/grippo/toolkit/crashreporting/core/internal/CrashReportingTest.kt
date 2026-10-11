package com.grippo.toolkit.crashreporting.core.internal

import com.grippo.toolkit.crashreporting.*
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

class CrashReportingTest {
    private class Sink : CrashSink {
        val reports = mutableListOf<Pair<Throwable, CrashContext>>()
        val crumbs = mutableListOf<CrashCode>()
        override fun record(throwable: Throwable, context: CrashContext) { reports += throwable to context }
        override fun breadcrumb(code: CrashCode) { crumbs += code }
    }
    @Test fun bootstrapBufferIsBoundedAndDrainedOnce() = runTest {
        val core = DefaultCrashReporting(2, StandardTestDispatcher(testScheduler)); val sink = Sink()
        core.record(Exception("one")); core.breadcrumb(CrashCode("two")); core.record(Exception("three"))
        assertEquals(1L, core.count(CrashReportingDisposition.Overflow))
        core.attachSink(sink); runCurrent()
        assertEquals(1, sink.reports.size); assertEquals(1, sink.crumbs.size)
        assertFailsWith<IllegalStateException> { core.attachSink(sink) }
        core.close()
    }
    @Test fun cancellationIsExcluded() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); val sink = Sink(); core.attachSink(sink)
        core.record(CancellationException()); runCurrent(); assertTrue(sink.reports.isEmpty())
        assertEquals(1L, core.count(CrashReportingDisposition.CancellationIgnored)); core.close()
    }
    @Test fun transportFailureDoesNotEscapeOrStopNextReport() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); var calls = 0
        core.attachSink(object : CrashSink {
            override fun record(throwable: Throwable, context: CrashContext) { if (++calls == 1) throw CancellationException() }
            override fun breadcrumb(code: CrashCode) = Unit
        })
        repeat(2) { core.record(Exception()) }; runCurrent()
        assertEquals(1L, core.count(CrashReportingDisposition.TransportFailure)); assertEquals(1L, core.count(CrashReportingDisposition.Submitted)); core.close()
    }
    @Test fun closeDropsBufferedAndFutureWork() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); core.record(Exception()); core.close(); core.record(Exception())
        assertEquals(2L, core.count(CrashReportingDisposition.Closed))
    }
    @Test fun contextHasSnapshotAndStrictBounds() {
        val list = mutableListOf<CrashAttribute>(CrashAttribute.Count(CrashCode("count"), 5))
        val context = CrashContext(list); list.clear(); assertEquals(1, context.attributes.size)
        assertFailsWith<IllegalArgumentException> { CrashCode("personal value") }
        assertFailsWith<IllegalArgumentException> { CrashAttribute.Count(CrashCode("count"), -1) }
        assertFailsWith<IllegalArgumentException> { CrashContext(List(2) { CrashAttribute.Flag(CrashCode("flag"), true) }) }
        assertFailsWith<IllegalArgumentException> { CrashContext(List(33) { CrashAttribute.Flag(CrashCode("key_$it"), true) }) }
    }
    @Test fun originalThrowableAndTypedContextReachSink() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); val sink = Sink(); core.attachSink(sink)
        val error = Exception("test"); val context = CrashContext(listOf(CrashAttribute.Flag(CrashCode("offline"), true)))
        core.record(error, context); runCurrent()
        assertSame(error, sink.reports.single().first); assertSame(context, sink.reports.single().second); core.close()
    }
    @Test fun parallelReportsAreSerializedAndLossesAccountedFor(): Unit = runBlocking {
        val core = DefaultCrashReporting(128); val sink = Sink(); core.attachSink(sink)
        try {
            List(4) { launch(Dispatchers.Default) { repeat(50) { core.record(Exception()) } } }.forEach { it.join() }
            withTimeout(5000) {
                while (core.count(CrashReportingDisposition.Submitted) + core.count(CrashReportingDisposition.Overflow) < 200) yield()
            }
            assertEquals(core.count(CrashReportingDisposition.Submitted), sink.reports.size.toLong())
            assertEquals(200L, core.count(CrashReportingDisposition.Submitted) + core.count(CrashReportingDisposition.Overflow))
        } finally { core.close() }
    }
    @Test fun oversizedContextFailsBeforeCopyingOrReadingItsElements() {
        val oversized = object : AbstractList<CrashAttribute>() {
            override val size = 33
            override fun get(index: Int): CrashAttribute = error("Oversized input must not be read")
        }
        assertFailsWith<IllegalArgumentException> { CrashContext(oversized) }
    }
    @Test fun recordingNeverInvokesSdkInCaller() = runTest {
        val sink = Sink(); val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); core.attachSink(sink)
        core.record(Exception("fixture")); assertTrue(sink.reports.isEmpty())
        runCurrent(); assertEquals(1, sink.reports.size); core.close()
    }
    @Test fun cancelledCallerStillJoinsWorker() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler))
        val job = launch { currentCoroutineContext().job.cancel(); core.close() }; job.join()
        assertFailsWith<IllegalStateException> { core.attachSink(Sink()) }
        core.record(Exception()); assertEquals(1L, core.count(CrashReportingDisposition.Closed))
    }
}
