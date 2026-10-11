package com.grippo.toolkit.analytics

import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.EntryPoint
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.internal.executeTrackedOperation
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.operation.trackOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.values.DurationMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class AnalyticsOperationTest {
    private val flow = FlowContext.start(EntryPoint("fixture_entry"), ScreenId("fixture_screen"))

    @Test fun completionSharesIdsAndMeasuresTheOperationWithoutFactoryTime() = runTest {
        val clock = TestTimeSource()
        val analytics = Recording()
        val events = Events(onStart = { clock += 100.milliseconds })
        val result = analytics.executeTrackedOperation(events, flow, clock) {
            clock += 250.milliseconds
            7
        }
        assertEquals(7, result)
        assertEquals(listOf("fixture_started", "fixture_completed"), analytics.entries.map { it.event.schema.name })
        assertEquals(250, events.duration?.value)
        assertEquals(7, events.result)
        assertSame(flow, analytics.entries[0].flow)
        assertSame(flow, analytics.entries[1].flow)
        assertEquals(analytics.entries[0].operation, analytics.entries[1].operation)
    }

    @Test fun failureRethrowsTheOriginalExceptionAndProducesOneTerminalEvent() = runTest {
        val analytics = Recording()
        val events = Events()
        val failure = IllegalArgumentException("private exception message")
        assertSame(failure, assertFailsWith<IllegalArgumentException> {
            analytics.trackOperation(events, flow) { throw failure }
        })
        assertSame(failure, events.error)
        assertEquals(listOf("fixture_started", "fixture_failed"), analytics.entries.map { it.event.schema.name })
    }

    @Test fun cancellationIsInterruptionAndDoesNotBecomeFailure() = runTest {
        val analytics = Recording()
        val events = Events()
        val task = async { analytics.trackOperation(events, flow) { awaitCancellation() } }
        runCurrent()
        task.cancelAndJoin()
        assertEquals(listOf("fixture_started", "fixture_interrupted"), analytics.entries.map { it.event.schema.name })
        assertEquals(null, events.error)
    }

    @Test fun anAlreadyCancelledCallerDoesNotStartAnOperation() = runTest {
        val analytics = Recording()
        val task = async {
            coroutineContext[kotlinx.coroutines.Job]!!.cancel()
            analytics.trackOperation(Events(), flow) { 7 }
        }
        assertFailsWith<CancellationException> { task.await() }
        assertEquals(emptyList(), analytics.entries)
    }

    @Test fun aBlockReturningAfterCancellationCannotReportCompletion() = runTest {
        val analytics = Recording()
        val task = async {
            analytics.trackOperation(Events(), flow) {
                coroutineContext[kotlinx.coroutines.Job]!!.cancel()
                7
            }
        }
        assertFailsWith<CancellationException> { task.await() }
        assertEquals(listOf("fixture_started", "fixture_interrupted"), analytics.entries.map { it.event.schema.name })
    }

    @Test fun cancellationBetweenStartAndTheBlockDoesNotRunBusinessWork() = runTest {
        val analytics = Recording()
        var ran = false
        val task = async {
            val job = coroutineContext[kotlinx.coroutines.Job]!!
            analytics.trackOperation(Events(onStart = { job.cancel() }), flow) { ran = true; 7 }
        }
        assertFailsWith<CancellationException> { task.await() }
        assertEquals(false, ran)
        assertEquals(listOf("fixture_started", "fixture_interrupted"), analytics.entries.map { it.event.schema.name })
    }

    @Test fun aTypedReturnedFailureIsPassedToTheFeatureClassifierRatherThanDeclaredSuccess() = runTest {
        val analytics = Recording()
        val failure = Result.failure<Int>(IllegalStateException("operation"))
        val events = object : AnalyticsOperationEvents<Result<Int>> {
            override fun started(): AnalyticsEvent = Event("fixture_started")
            override fun completed(result: Result<Int>, duration: DurationMillis): AnalyticsEvent {
                assertEquals(failure, result)
                return Event(if (result.isSuccess) "fixture_completed" else "fixture_failed")
            }
            override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
                error("Returned failure must reach completed as a typed result")
            override fun interrupted(duration: DurationMillis): AnalyticsEvent =
                error("Returned failure is not interruption")
        }
        assertEquals(failure, analytics.trackOperation(events, flow) { failure })
        assertEquals(listOf("fixture_started", "fixture_failed"), analytics.entries.map { it.event.schema.name })
    }

    @Test fun parallelAttemptsHaveDistinctIdsAndKeepTheirOwnFlow() = runTest {
        val analytics = Recording()
        val otherFlow = FlowContext.start(EntryPoint("other_entry"), null)
        val ready = CompletableDeferred<Unit>()
        val first = async { analytics.trackOperation(Events(), flow) { ready.await(); 1 } }
        runCurrent()
        val second = async { analytics.trackOperation(Events(), otherFlow) { 2 } }
        second.await()
        ready.complete(Unit)
        first.await()
        val firstEntries = analytics.entries.filter { it.flow == flow }
        val secondEntries = analytics.entries.filter { it.flow == otherFlow }
        assertEquals(2, firstEntries.size)
        assertEquals(2, secondEntries.size)
        assertEquals(firstEntries[0].operation, firstEntries[1].operation)
        assertEquals(secondEntries[0].operation, secondEntries[1].operation)
        assertNotEquals(firstEntries[0].operation, secondEntries[0].operation)
        assertNotEquals(flow.id, otherFlow.id)
    }

    @Test fun terminalEncodingFailureCannotCreateASecondTerminalEvent() = runTest {
        val analytics = Recording()
        val events = object : AnalyticsOperationEvents<Int> by Events() {
            override fun completed(result: Int, duration: DurationMillis): AnalyticsEvent = error("schema defect")
        }
        assertFailsWith<IllegalStateException> { analytics.trackOperation(events, flow) { 1 } }
        assertEquals(listOf("fixture_started"), analytics.entries.map { it.event.schema.name })
    }

    @Test fun reportingFailureIsAttachedWithoutReplacingTheOriginalOperationError() = runTest {
        val analytics = Recording()
        val failure = IllegalArgumentException("operation")
        val encodingFailure = IllegalStateException("schema defect")
        val events = object : AnalyticsOperationEvents<Int> by Events() {
            override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent = throw encodingFailure
        }
        assertSame(failure, assertFailsWith<IllegalArgumentException> {
            analytics.trackOperation(events, flow) { throw failure }
        })
        assertEquals(listOf(encodingFailure), failure.suppressedExceptions)
        assertEquals(1, analytics.entries.size)
    }

    private class Events(private val onStart: () -> Unit = {}) : AnalyticsOperationEvents<Int> {
        var result: Int? = null
        var error: Exception? = null
        var duration: DurationMillis? = null
        override fun started(): AnalyticsEvent { onStart(); return Event("fixture_started") }
        override fun completed(result: Int, duration: DurationMillis): AnalyticsEvent {
            this.result = result; this.duration = duration
            return Event("fixture_completed")
        }
        override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent {
            this.error = error; this.duration = duration
            return Event("fixture_failed")
        }
        override fun interrupted(duration: DurationMillis): AnalyticsEvent {
            this.duration = duration
            return Event("fixture_interrupted")
        }
    }

    private class Event(name: String) : AnalyticsEvent {
        override val schema = AnalyticsEventSchema(name, 1, emptyList())
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
    private class Entry(val event: AnalyticsEvent, val flow: FlowContext?, val operation: OperationId?)
    private class Recording : Analytics {
        val entries = mutableListOf<Entry>()
        override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
            entries.add(Entry(event, flow, operation))
        }
        override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation =
            error("Screen presentation is not used by operation fixtures")
        override fun updateContext(context: AnalyticsContext) = Unit
        override fun updateIdentity(identity: AnalyticsIdentity) = Unit
        override fun updateCollection(collection: AnalyticsCollection) = Unit
    }
}
