package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.scenario.AuthAnalyticsImpl
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.EntryPoint
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.analytics.authorization.models.Method
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.*
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

class AuthOperationsTest {
    private suspend fun suspendFailure(error: Exception): String = throw error

    @Test fun allMethodsReturnWholeLoginResultAndNeverEncodeCredentials() = runTest {
        for (method in Method.entries) {
            val recorder = RecordingAuthAnalytics()
            val facade = authorization(recorder)
            var serverValue: String? = null
            assertTrue(facade.trackOperation(AuthOperation.Login(method)) {
                val credential = "secret_token"
                serverValue = credential
                true
            })
            assertEquals("secret_token", serverValue)
            assertEquals(listOf("auth_flow_started", "auth_attempt_started", "auth_attempt_succeeded"), recorder.names)
            assertTrue(recorder.events.all { it.flow?.id == facade.save().flow.id })
            assertFalse(recorder.events.flatMap { it.event.parameters() }.any { "secret" in it.value.toString() })
        }
    }
    @Test fun providerCancellationNeverCallsServerAndPreservesOriginal() = runTest {
        val recorder = RecordingAuthAnalytics()
        val facade = authorization(recorder)
        val original = TestFailure(FailureCategory.Unexpected, cancelled = true)
        var called = false
        assertSame(original, assertFailsWith<TestFailure> {
            facade.trackOperation(AuthOperation.Login(Method.Apple)) {
                val credential = suspendFailure(original)
                called = true
                assertNotNull(credential)
            }
        })
        assertFalse(called)
        assertEquals("auth_attempt_cancelled", recorder.names.last())
    }
    @Test fun exceptionTextDoesNotDetermineCancellation() = runTest {
        val recorder = RecordingAuthAnalytics()
        val original = IllegalStateException("cancelled secret")
        assertSame(original, assertFailsWith<IllegalStateException> {
            authorization(recorder).trackOperation(AuthOperation.Login(Method.Google)) {
                throw original
            }
        })
        assertEquals("auth_attempt_failed", recorder.names.last())
        assertNull(recorder.events.last().text("failure_stage"))
        assertEquals("unexpected", recorder.events.last().text("failure_category"))
    }
    @Test fun serverRetryHasExactlyOneTerminalAndFreshIds() = runTest {
        val recorder = RecordingAuthAnalytics()
        val facade = authorization(recorder)
        assertFailsWith<TestFailure> {
            facade.trackOperation(AuthOperation.Login(Method.Email)) {
                throw TestFailure(FailureCategory.Rejected)
            }
        }
        facade.trackOperation(AuthOperation.Login(Method.Email)) {}
        val attempts = recorder.events.drop(1)
        assertEquals(listOf("auth_attempt_started", "auth_attempt_failed", "auth_attempt_started", "auth_attempt_succeeded"), attempts.map { it.event.schema.name })
        assertEquals(attempts[0].operation, attempts[1].operation)
        assertEquals(attempts[2].operation, attempts[3].operation)
        assertNotEquals(attempts[0].operation, attempts[2].operation)
        assertNull(attempts[1].text("failure_stage"))
    }
    @Test fun cancellationInEachWholeOperationProducesInterruption() = runTest {
        for (operation in listOf("attempt", "session")) {
            val recorder = RecordingAuthAnalytics()
            val job = launch {
                when (operation) {
                    "attempt" -> authorization(recorder).trackOperation(AuthOperation.Login(Method.Google)) {
                        awaitCancellation()
                    }
                    "session" -> session(recorder).trackOperation(AuthOperation.SessionCheck) { awaitCancellation() }
                }
            }
            runCurrent(); job.cancelAndJoin()
            val events = recorder.events.filter { it.operation != null }
            assertEquals(2, events.size)
            assertTrue(events.last().event.schema.name.endsWith("_interrupted"))
            assertEquals(events.first().operation, events.last().operation)
        }
    }
    @Test fun thrownCancellationRemainsInterruptionWithActiveCaller() = runTest {
        val recorder = RecordingAuthAnalytics()
        val original = CancellationException("private")
        assertSame(original, assertFailsWith<CancellationException> {
            authorization(recorder).trackOperation(AuthOperation.Login(Method.Google)) {
                throw original
            }
        })
        currentCoroutineContext().ensureActive()
        assertEquals("auth_attempt_interrupted", recorder.names.last())
    }
    @Test fun parallelScenariosDoNotMixContexts() = runTest {
        val recorder = RecordingAuthAnalytics()
        val first = authorization(recorder); val second = authorization(recorder)
        val release = CompletableDeferred<Unit>()
        val jobs = listOf(first, second).map { facade ->
            async {
                facade.trackOperation(AuthOperation.Login(Method.Email)) { release.await() }
            }
        }
        runCurrent(); release.complete(Unit); jobs.awaitAll()
        val contexts = listOf(first.save().flow, second.save().flow)
        assertNotEquals(contexts[0].id, contexts[1].id)
        val attempts = contexts.map { context -> recorder.events.filter { it.flow == context && it.operation != null } }
        attempts.forEach { assertEquals(2, it.size); assertEquals(it[0].operation, it[1].operation) }
        assertNotEquals(attempts[0][0].operation, attempts[1][0].operation)
    }
    @Test fun sessionReturnsBusinessValueUnchangedAndPreservesErrors() = runTest {
        val recorder = RecordingAuthAnalytics()
        val result = Any()
        assertSame(result, session(recorder).trackOperation(AuthOperation.SessionCheck) { result })
        assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), recorder.names)
        assertNull(recorder.events.last().text("session_outcome"))
        val original = TestFailure(FailureCategory.Timeout)
        assertSame(original, assertFailsWith<TestFailure> {
            session(recorder).trackOperation(AuthOperation.SessionCheck) { throw original }
        })
        assertEquals("auth_session_check_failed", recorder.names.last())
        assertEquals("timeout", recorder.events.last().text("failure_category"))
        assertNull(recorder.events.last().text("failure_stage"))
    }
    @Test fun reportingDefectStaysAttachedToOriginalBusinessException() = runTest {
        val recorder = RecordingAuthAnalytics()
        val defect = IllegalStateException("encoding defect")
        val original = TestFailure(FailureCategory.Timeout)
        val analytics = object : Analytics by recorder {
            override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
                recorder.track(event, flow, operation)
                if (event.schema.name == "auth_attempt_failed") throw defect
            }
        }
        val facade = AuthAnalyticsImpl(analytics, TestClassifier).authorization(EntryPoint.TokenAbsent, FlowId.create(), null)
        assertSame(original, assertFailsWith<TestFailure> { facade.trackOperation(AuthOperation.Register) { throw original } })
        assertTrue(original.suppressedExceptions.any { it === defect })
    }

}
