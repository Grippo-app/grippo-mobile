package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.AuthAnalyticsImpl
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.Method
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class AuthOperationsTest {
    private val operations = listOf(
        AuthOperation.Login(Method.Email),
        AuthOperation.Login(Method.Google),
        AuthOperation.Login(Method.Apple),
        AuthOperation.Register,
        AuthOperation.SessionCheck,
        AuthOperation.CreateProfile,
        AuthOperation.Logout,
    )

    @Test
    fun everyOperationReturnsTheExactBusinessValueAndSharesOneFreshOutcomeId() = runTest {
        val recorder = RecordingAuthAnalytics()
        val analytics = AuthAnalyticsImpl(recorder)
        val result = Any()
        operations.forEach { operation ->
            assertSame(result, analytics.trackOperation(operation) {
                result
            })
        }
        val expectedNames = listOf(
            "auth_attempt_started", "auth_attempt_succeeded",
            "auth_attempt_started", "auth_attempt_succeeded",
            "auth_attempt_started", "auth_attempt_succeeded",
            "auth_attempt_started", "auth_attempt_succeeded",
            "auth_session_check_started", "auth_session_check_completed",
            "profile_operation_started", "profile_operation_completed",
            "profile_operation_started", "profile_operation_completed",
        )
        assertEquals(expectedNames, recorder.names)
        recorder.events.chunked(2).forEach { pair ->
            assertNotNull(pair.first().operation)
            assertEquals(pair.first().operation, pair.last().operation)
            assertNull(pair.first().flow)
            assertNull(pair.last().flow)
            val duration = pair.last().event.parameters().last().value as AnalyticsValue.Integer
            assertTrue(duration.value >= 0)
        }
        assertEquals(operations.size, recorder.events.mapNotNull { it.operation }.distinct().size)
        assertEquals("register", recorder.events[6].text("auth_action"))
        assertEquals("email", recorder.events[6].text("auth_method"))
        assertEquals("create", recorder.events[10].text("profile_action"))
        assertEquals("logout", recorder.events[12].text("profile_action"))
    }

    @Test
    fun businessErrorsKeepTheirIdentityWithoutPrivateTextOrClassification() = runTest {
        operations.forEach { operation ->
            val recorder = RecordingAuthAnalytics()
            val original = IllegalStateException("private credential token cancelled network")
            assertSame(original, assertFailsWith<IllegalStateException> {
                AuthAnalyticsImpl(recorder).trackOperation(operation) {
                    throw original
                }
            })
            assertEquals(2, recorder.events.size)
            assertTrue(recorder.names.last().endsWith("_failed"))
            assertEquals(recorder.events.first().operation, recorder.events.last().operation)
            assertTrue(recorder.events.all { it.flow == null })
            val parameters = recorder.events.flatMap { it.event.parameters() }
            assertTrue(parameters.none { it.key.name in setOf("failure_category", "failure_stage", "session_outcome") })
            assertTrue(parameters.none { "private" in it.value.toString() || "token" in it.value.toString() })
        }
    }

    @Test
    fun thrownCoroutineCancellationRemainsInterruptionForEveryOperation() = runTest {
        operations.forEach { operation ->
            val recorder = RecordingAuthAnalytics()
            val original = CancellationException("private cancellation")
            assertSame(original, assertFailsWith<CancellationException> {
                AuthAnalyticsImpl(recorder).trackOperation(operation) {
                    throw original
                }
            })
            assertEquals(2, recorder.events.size)
            assertTrue(recorder.names.last().endsWith("_interrupted"))
            assertEquals(recorder.events.first().operation, recorder.events.last().operation)
        }
    }

    @Test
    fun cancellationOfCallerProducesOneInterruptedOutcome() = runTest {
        operations.forEach { operation ->
            val recorder = RecordingAuthAnalytics()
            val job = launch {
                AuthAnalyticsImpl(recorder).trackOperation(operation) {
                    awaitCancellation()
                }
            }
            runCurrent()
            job.cancelAndJoin()
            assertEquals(2, recorder.events.size)
            assertTrue(recorder.names.last().endsWith("_interrupted"))
            assertEquals(recorder.events.first().operation, recorder.events.last().operation)
        }
    }

    @Test
    fun userRetryStartsANewOperationWithoutHiddenRetryOrDuplicateTerminals() = runTest {
        val recorder = RecordingAuthAnalytics()
        val analytics = AuthAnalyticsImpl(recorder)
        var businessCalls = 0
        assertFailsWith<IllegalArgumentException> {
            analytics.trackOperation(AuthOperation.Register) {
                businessCalls++
                throw IllegalArgumentException("rejected")
            }
        }
        assertEquals(1, businessCalls)
        analytics.trackOperation(AuthOperation.Register) {
            businessCalls++
        }
        assertEquals(2, businessCalls)
        assertEquals(
            listOf("auth_attempt_started", "auth_attempt_failed", "auth_attempt_started", "auth_attempt_succeeded"),
            recorder.names,
        )
        assertNotEquals(recorder.events[0].operation, recorder.events[2].operation)
    }

    @Test
    fun oneStatelessFacadeSupportsConcurrentOperationsWithIndependentIds() = runTest {
        val recorder = RecordingAuthAnalytics()
        val analytics = AuthAnalyticsImpl(recorder)
        val release = CompletableDeferred<Unit>()
        val jobs = listOf(AuthOperation.Login(Method.Email), AuthOperation.CreateProfile).map { operation ->
            async {
                analytics.trackOperation(operation) {
                    release.await()
                }
            }
        }
        runCurrent()
        release.complete(Unit)
        jobs.awaitAll()
        val groups = recorder.events.groupBy { it.operation }
        assertEquals(2, groups.size)
        groups.values.forEach { events ->
            assertEquals(2, events.size)
            assertTrue(events.first().event.schema.name.endsWith("_started"))
            assertTrue(events.last().event.schema.name.endsWith("_succeeded") || events.last().event.schema.name.endsWith("_completed"))
            assertTrue(events.all { it.flow == null })
        }
    }

    @Test
    fun reportingFailureAtStartOrCompletionCannotReplaceTheBusinessResult() = runTest {
        val recorder = RecordingAuthAnalytics()
        val observer = object : Analytics by recorder {
            override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
                recorder.track(event, flow, operation)
                throw IllegalStateException("reporting failure")
            }
        }
        val result = Any()
        assertSame(result, AuthAnalyticsImpl(observer).trackOperation(AuthOperation.SessionCheck) {
            result
        })
        assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), recorder.names)
    }

    @Test
    fun failedReportingCannotReplaceOriginalBusinessException() = runTest {
        val recorder = RecordingAuthAnalytics()
        val defect = IllegalStateException("encoding defect")
        val original = IllegalArgumentException("business failure")
        val observer = object : Analytics by recorder {
            override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
                recorder.track(event, flow, operation)
                if (event.schema.name == "profile_operation_failed") throw defect
            }
        }
        assertSame(original, assertFailsWith<IllegalArgumentException> {
            AuthAnalyticsImpl(observer).trackOperation(AuthOperation.CreateProfile) {
                throw original
            }
        })
        assertTrue(original.suppressedExceptions.any { it === defect })
        assertEquals(2, recorder.events.size)
    }

    @Test
    fun fatalBusinessErrorsAreNotClassifiedAsRecoverableFailures() = runTest {
        val recorder = RecordingAuthAnalytics()
        val original = AssertionError("fatal")
        assertSame(original, assertFailsWith<AssertionError> {
            AuthAnalyticsImpl(recorder).trackOperation(AuthOperation.Logout) {
                throw original
            }
        })
        assertEquals(listOf("profile_operation_started"), recorder.names)
    }
}
