package com.grippo.toolkit.crashreporting.core.internal

import com.grippo.toolkit.crashreporting.*
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.metadata.CrashMetadataEntry
import com.grippo.toolkit.crashreporting.metadata.CrashMetadataKey
import com.grippo.toolkit.crashreporting.transport.CrashReportingSink
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

class CrashReportingTest {
    private class Sink : CrashReportingSink {
        val reports = mutableListOf<Pair<Throwable, CrashReportContext>>()
        val crumbs = mutableListOf<CrashDiagnosticCode>()
        override fun reportException(throwable: Throwable, context: CrashReportContext) { reports += throwable to context }
        override fun logBreadcrumb(code: CrashDiagnosticCode) { crumbs += code }
    }
    @Test fun bootstrapBufferIsBoundedAndDrainedOnce() = runTest {
        val core = DefaultCrashReporting(2, StandardTestDispatcher(testScheduler)); val sink = Sink()
        core.reportException(Exception("one")); core.logBreadcrumb(CrashDiagnosticCode("two")); core.reportException(Exception("three"))
        assertEquals(1L, core.count(CrashReportingDisposition.Overflow))
        core.attachSink(sink); runCurrent()
        assertEquals(1, sink.reports.size); assertEquals(1, sink.crumbs.size)
        assertFailsWith<IllegalStateException> { core.attachSink(sink) }
        core.close()
    }
    @Test fun cancellationIsExcluded() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); val sink = Sink(); core.attachSink(sink)
        core.reportException(CancellationException()); runCurrent(); assertTrue(sink.reports.isEmpty())
        assertEquals(1L, core.count(CrashReportingDisposition.CancellationIgnored)); core.close()
    }
    @Test fun transportFailureDoesNotEscapeOrStopNextReport() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); var calls = 0
        core.attachSink(object : CrashReportingSink {
            override fun reportException(throwable: Throwable, context: CrashReportContext) { if (++calls == 1) throw CancellationException() }
            override fun logBreadcrumb(code: CrashDiagnosticCode) = Unit
        })
        repeat(2) { core.reportException(Exception()) }; runCurrent()
        assertEquals(1L, core.count(CrashReportingDisposition.TransportFailure)); assertEquals(1L, core.count(CrashReportingDisposition.Submitted)); core.close()
    }
    @Test fun closeDropsBufferedAndFutureWork() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); core.reportException(Exception()); core.close(); core.reportException(Exception())
        assertEquals(2L, core.count(CrashReportingDisposition.Closed))
    }
    @Test fun contextHasSnapshotAndStrictBounds() {
        val list = mutableListOf<CrashMetadataEntry>(CrashMetadataKey.count("count").value(5))
        val context = CrashReportContext(list); list.clear(); assertEquals(1, context.metadata.size)
        assertFailsWith<IllegalArgumentException> { CrashDiagnosticCode("personal value") }
        assertFailsWith<IllegalArgumentException> { CrashMetadataKey.flag("personal value") }
        assertFailsWith<IllegalArgumentException> { CrashMetadataKey.count("count").value(-1) }
        assertFailsWith<IllegalArgumentException> { CrashReportContext(List(2) { CrashMetadataKey.flag("flag").value(true) }) }
        assertFailsWith<IllegalArgumentException> { CrashReportContext(List(33) { CrashMetadataKey.flag("key_$it").value(true) }) }
    }
    @Test fun originalThrowableAndTypedContextReachSink() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); val sink = Sink(); core.attachSink(sink)
        val error = Exception("test"); val context = CrashReportContext(listOf(CrashMetadataKey.flag("offline").value(true)))
        core.reportException(error, context); runCurrent()
        assertSame(error, sink.reports.single().first); assertSame(context, sink.reports.single().second); core.close()
    }
    @Test fun parallelReportsAreSerializedAndLossesAccountedFor(): Unit = runBlocking {
        val core = DefaultCrashReporting(128); val sink = Sink(); core.attachSink(sink)
        try {
            List(4) { launch(Dispatchers.Default) { repeat(50) { core.reportException(Exception()) } } }.forEach { it.join() }
            withTimeout(5000) {
                while (core.count(CrashReportingDisposition.Submitted) + core.count(CrashReportingDisposition.Overflow) < 200) yield()
            }
            assertEquals(core.count(CrashReportingDisposition.Submitted), sink.reports.size.toLong())
            assertEquals(200L, core.count(CrashReportingDisposition.Submitted) + core.count(CrashReportingDisposition.Overflow))
        } finally { core.close() }
    }
    @Test fun oversizedContextFailsBeforeCopyingOrReadingItsElements() {
        val oversized = object : AbstractList<CrashMetadataEntry>() {
            override val size = 33
            override fun get(index: Int): CrashMetadataEntry = error("Oversized input must not be read")
        }
        assertFailsWith<IllegalArgumentException> { CrashReportContext(oversized) }
    }
    @Test fun recordingNeverInvokesSdkInCaller() = runTest {
        val sink = Sink(); val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler)); core.attachSink(sink)
        core.reportException(Exception("fixture")); assertTrue(sink.reports.isEmpty())
        runCurrent(); assertEquals(1, sink.reports.size); core.close()
    }
    @Test fun cancelledCallerStillJoinsWorker() = runTest {
        val core = DefaultCrashReporting(dispatcher = StandardTestDispatcher(testScheduler))
        val job = launch { currentCoroutineContext().job.cancel(); core.close() }; job.join()
        assertFailsWith<IllegalStateException> { core.attachSink(Sink()) }
        core.reportException(Exception()); assertEquals(1L, core.count(CrashReportingDisposition.Closed))
    }
}
