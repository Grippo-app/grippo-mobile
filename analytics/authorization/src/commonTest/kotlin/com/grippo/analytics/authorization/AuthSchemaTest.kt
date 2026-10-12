package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.events.AuthAttemptEvent
import com.grippo.analytics.authorization.internal.events.AuthFlowEvent
import com.grippo.analytics.authorization.internal.events.AuthSessionEvent
import com.grippo.analytics.authorization.internal.models.AuthAction
import com.grippo.analytics.authorization.internal.schema.AuthEventSchema
import com.grippo.analytics.authorization.models.ExitReason
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.analytics.authorization.models.Method
import com.grippo.analytics.authorization.models.Step
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import com.grippo.toolkit.analytics.values.DurationMillis
import kotlin.test.*
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

class AuthSchemaTest {
    @Test fun annotatedModuleContributesExactlyTheFeatureSchemaMatrix() {
        val application = koinApplication { modules(AuthAnalyticsModule().module) }
        try {
            val schemas = application.koin.getAll<AnalyticsSchema>()
            assertEquals(1, schemas.size)
            assertEquals(AuthEventSchema.all, schemas.single().events)
            assertEquals(12, schemas.single().events.size)
            assertEquals(12, schemas.single().events.map { it.name }.distinct().size)
        } finally { application.close() }
    }

    @Test fun everyEventHasItsExactStableWireFixtureAndUsesItsDeclaredKeyInstances() {
        val duration = DurationMillis(42)
        val events = listOf(
            AuthAttemptEvent.Started(Method.Email, AuthAction.Login),
            AuthAttemptEvent.Succeeded(Method.Google, AuthAction.Login, duration),
            AuthAttemptEvent.Failed(Method.Email, AuthAction.Login, duration, FailureCategory.Rejected),
            AuthAttemptEvent.Interrupted(Method.Email, AuthAction.Login, duration),
            AuthAttemptEvent.Cancelled(Method.Apple, AuthAction.Login, duration),
            AuthSessionEvent.Started,
            AuthSessionEvent.Completed(duration),
            AuthSessionEvent.Failed(duration, FailureCategory.Unexpected),
            AuthSessionEvent.Interrupted(duration),
            AuthFlowEvent.FlowStarted,
            AuthFlowEvent.FlowExited(ExitReason.UserBack, Step.Login),
            AuthFlowEvent.StepChanged(Step.Login, Step.Registration, NavigationReason.Action),
        )
        val fixtures = linkedMapOf(
            "auth_attempt_started" to listOf("auth_method", "auth_action"),
            "auth_attempt_succeeded" to listOf("auth_method", "auth_action", "duration_ms"),
            "auth_attempt_failed" to listOf("auth_method", "auth_action", "duration_ms", "failure_category"),
            "auth_attempt_interrupted" to listOf("auth_method", "auth_action", "duration_ms"),
            "auth_attempt_cancelled" to listOf("auth_method", "auth_action", "duration_ms"),
            "auth_session_check_started" to emptyList(),
            "auth_session_check_completed" to listOf("duration_ms"),
            "auth_session_check_failed" to listOf("duration_ms", "failure_category"),
            "auth_session_check_interrupted" to listOf("duration_ms"),
            "auth_flow_started" to emptyList(),
            "auth_flow_exited" to listOf("exit_reason", "auth_step"),
            "auth_step_changed" to listOf("from_step", "to_step", "transition_reason"),
        )
        assertEquals(fixtures.keys, AuthEventSchema.all.map { it.name }.toSet())
        for (event in events) {
            assertSame(AuthEventSchema.all.single { it.name == event.schema.name }, event.schema)
            val expectedVersion = when {
                event.schema.name == "auth_attempt_failed" -> 4
                event.schema.name.startsWith("auth_attempt_") -> 3
                event.schema.name.startsWith("auth_session_check_") -> 3
                else -> 1
            }
            assertEquals(expectedVersion, event.schema.version)
            assertEquals(fixtures[event.schema.name], event.parameters().map { it.key.name })
            assertEquals(event.schema.parameters.size, event.parameters().size)
            event.parameters().zip(event.schema.parameters).forEach { (parameter, key) -> assertSame(key, parameter.key) }
        }
        assertEquals(42L, (events[1].parameters().last().value as com.grippo.toolkit.analytics.transport.AnalyticsValue.Integer).value)
    }


}
