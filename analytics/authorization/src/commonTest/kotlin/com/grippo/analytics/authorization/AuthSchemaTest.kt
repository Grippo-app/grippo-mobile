package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.operation.AuthOperationEvents
import com.grippo.analytics.authorization.internal.schema.AuthInteractionSchema
import com.grippo.analytics.authorization.internal.schema.AuthOperationSchema
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.Method
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import com.grippo.toolkit.analytics.values.DurationMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AuthSchemaTest {
    @Test
    fun catalogContainsOnlyTheCurrentTypedEventsWithExplicitFailureVersionChanges() {
        val fixtures = linkedMapOf(
            "auth_attempt_started" to (3 to listOf("auth_method", "auth_action")),
            "auth_attempt_succeeded" to (3 to listOf("auth_method", "auth_action", "duration_ms")),
            "auth_attempt_failed" to (5 to listOf("auth_method", "auth_action", "duration_ms")),
            "auth_attempt_interrupted" to (3 to listOf("auth_method", "auth_action", "duration_ms")),
            "auth_session_check_started" to (3 to emptyList()),
            "auth_session_check_completed" to (3 to listOf("duration_ms")),
            "auth_session_check_failed" to (4 to listOf("duration_ms")),
            "auth_session_check_interrupted" to (3 to listOf("duration_ms")),
            "profile_operation_started" to (1 to listOf("profile_action")),
            "profile_operation_completed" to (1 to listOf("profile_action", "duration_ms")),
            "profile_operation_failed" to (2 to listOf("profile_action", "duration_ms")),
            "profile_operation_interrupted" to (1 to listOf("profile_action", "duration_ms")),
            "auth_control_clicked" to (1 to listOf("interaction_screen", "interaction_action")),
            "auth_submit_clicked" to (1 to listOf("interaction_screen", "auth_method")),
            "auth_input_changed" to (1 to listOf("interaction_screen", "input_field", "input_status")),
            "auth_input_focused" to (1 to listOf("interaction_screen", "input_field")),
            "auth_input_cleared" to (1 to listOf("interaction_screen", "input_field")),
            "auth_keyboard_action" to (1 to listOf("interaction_screen", "input_field", "keyboard_action")),
            "auth_password_visibility_changed" to (1 to listOf("interaction_screen", "password_visible")),
            "auth_picker_opened" to (1 to listOf("interaction_screen", "input_field")),
            "auth_experience_selected" to (1 to listOf("interaction_screen", "profile_experience")),
            "auth_selection_changed" to (1 to listOf("interaction_screen", "selected", "selected_count")),
        )
        val catalog = AuthOperationSchema.all + AuthInteractionSchema.all
        assertEquals(22, catalog.size)
        assertEquals(fixtures.keys.toList(), catalog.map { it.name })
        catalog.forEach { schema ->
            val fixture = fixtures.getValue(schema.name)
            assertEquals(fixture.first, schema.version)
            assertEquals(fixture.second, schema.parameters.map { it.name })
            assertTrue(schema.parameters.all { it.required })
        }
    }

    @Test
    fun everyOperationFactoryUsesCanonicalSchemasKeysAndDurationWithoutBusinessValues() {
        val duration = DurationMillis(42)
        val operations = listOf(
            AuthOperation.Login(Method.Apple),
            AuthOperation.Register,
            AuthOperation.SessionCheck,
            AuthOperation.CreateProfile,
            AuthOperation.Logout,
        )
        val observed = mutableSetOf<String>()
        operations.forEach { operation ->
            val events = AuthOperationEvents<Any>(operation)
            val payloads = listOf(
                events.started(),
                events.completed(Any(), duration),
                events.failed(IllegalStateException("private token"), duration),
                events.interrupted(duration),
            )
            payloads.forEach { payload ->
                observed += payload.schema.name
                assertSame(AuthOperationSchema.all.single { it.name == payload.schema.name }, payload.schema)
                assertEquals(payload.schema.parameters.size, payload.parameters().size)
                payload.parameters().zip(payload.schema.parameters).forEach { (parameter, key) ->
                    assertSame(key, parameter.key)
                }
            }
            payloads.drop(1).forEach { payload ->
                assertEquals(AnalyticsValue.Integer(42), payload.parameters().last().value)
            }
            assertEquals(
                payloads.first().parameters().map { it.value },
                payloads.last().parameters().dropLast(1).map { it.value },
            )
            val expectedBase = when (operation) {
                is AuthOperation.Login -> listOf(AnalyticsValue.Text("apple"), AnalyticsValue.Text("login"))
                AuthOperation.Register -> listOf(AnalyticsValue.Text("email"), AnalyticsValue.Text("register"))
                AuthOperation.SessionCheck -> emptyList()
                AuthOperation.CreateProfile -> listOf(AnalyticsValue.Text("create"))
                AuthOperation.Logout -> listOf(AnalyticsValue.Text("logout"))
            }
            assertEquals(expectedBase, payloads.first().parameters().map { it.value })
        }
        assertEquals(AuthOperationSchema.all.map { it.name }.toSet(), observed)
    }
}
