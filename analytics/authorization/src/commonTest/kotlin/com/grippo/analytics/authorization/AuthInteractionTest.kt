package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.AuthAnalyticsImpl
import com.grippo.analytics.authorization.internal.schema.AuthInteractionSchema
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.CredentialField
import com.grippo.analytics.authorization.models.Method
import com.grippo.analytics.authorization.models.ProfileExperience
import com.grippo.analytics.authorization.models.ProfilePicker
import com.grippo.analytics.common.models.ControlAction
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.analytics.common.models.InputStatus
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AuthInteractionTest {
    private data class Case(
        val fact: AuthEvent,
        val screen: String,
        val schema: String,
        val parameters: Map<String, AnalyticsValue> = emptyMap(),
    )

    @Test
    fun everyTypedFactHasItsExactBoundedPayloadAndExplicitScreen() {
        val cases = listOf(
            Case(
                AuthEvent.Splash.Back,
                "auth_splash",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("back"),
                ),
            ),
            Case(
                AuthEvent.Login.InputChanged(CredentialField.Email, InputStatus.Invalid),
                "auth_login",
                "auth_input_changed",
                mapOf(
                    "input_field" to AnalyticsValue.Text("email"),
                    "input_status" to AnalyticsValue.Text("invalid"),
                ),
            ),
            Case(
                AuthEvent.Login.InputFocused(CredentialField.Email),
                "auth_login",
                "auth_input_focused",
                mapOf(
                    "input_field" to AnalyticsValue.Text("email"),
                ),
            ),
            Case(
                AuthEvent.Login.InputCleared(CredentialField.Email),
                "auth_login",
                "auth_input_cleared",
                mapOf(
                    "input_field" to AnalyticsValue.Text("email"),
                ),
            ),
            Case(
                AuthEvent.Login.KeyboardAction(CredentialField.Password, InputKeyboardAction.Done),
                "auth_login",
                "auth_keyboard_action",
                mapOf(
                    "input_field" to AnalyticsValue.Text("password"),
                    "keyboard_action" to AnalyticsValue.Text("done"),
                ),
            ),
            Case(
                AuthEvent.Login.PasswordVisibilityChanged(true),
                "auth_login",
                "auth_password_visibility_changed",
                mapOf(
                    "password_visible" to AnalyticsValue.Integer(1),
                ),
            ),
            Case(
                AuthEvent.Login.Submit(Method.Google),
                "auth_login",
                "auth_submit_clicked",
                mapOf(
                    "auth_method" to AnalyticsValue.Text("google"),
                ),
            ),
            Case(
                AuthEvent.Login.Register,
                "auth_login",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("register"),
                ),
            ),
            Case(
                AuthEvent.Login.Back,
                "auth_login",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("back"),
                ),
            ),
            Case(
                AuthEvent.Registration.InputChanged(CredentialField.Password, InputStatus.Valid),
                "auth_registration",
                "auth_input_changed",
                mapOf(
                    "input_field" to AnalyticsValue.Text("password"),
                    "input_status" to AnalyticsValue.Text("valid"),
                ),
            ),
            Case(
                AuthEvent.Registration.InputFocused(CredentialField.Password),
                "auth_registration",
                "auth_input_focused",
                mapOf(
                    "input_field" to AnalyticsValue.Text("password"),
                ),
            ),
            Case(
                AuthEvent.Registration.InputCleared(CredentialField.Email),
                "auth_registration",
                "auth_input_cleared",
                mapOf(
                    "input_field" to AnalyticsValue.Text("email"),
                ),
            ),
            Case(
                AuthEvent.Registration.KeyboardAction(CredentialField.Email, InputKeyboardAction.Next),
                "auth_registration",
                "auth_keyboard_action",
                mapOf(
                    "input_field" to AnalyticsValue.Text("email"),
                    "keyboard_action" to AnalyticsValue.Text("next"),
                ),
            ),
            Case(
                AuthEvent.Registration.PasswordVisibilityChanged(false),
                "auth_registration",
                "auth_password_visibility_changed",
                mapOf(
                    "password_visible" to AnalyticsValue.Integer(0),
                ),
            ),
            Case(
                AuthEvent.Registration.Submit,
                "auth_registration",
                "auth_submit_clicked",
                mapOf(
                    "auth_method" to AnalyticsValue.Text("email"),
                ),
            ),
            Case(
                AuthEvent.Registration.Back,
                "auth_registration",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("back"),
                ),
            ),
            Case(
                AuthEvent.User.NameInputChanged(InputStatus.Valid),
                "profile_creation_user",
                "auth_input_changed",
                mapOf(
                    "input_field" to AnalyticsValue.Text("name"),
                    "input_status" to AnalyticsValue.Text("valid"),
                ),
            ),
            Case(
                AuthEvent.User.NameInputFocused,
                "profile_creation_user",
                "auth_input_focused",
                mapOf(
                    "input_field" to AnalyticsValue.Text("name"),
                ),
            ),
            Case(
                AuthEvent.User.NameInputCleared,
                "profile_creation_user",
                "auth_input_cleared",
                mapOf(
                    "input_field" to AnalyticsValue.Text("name"),
                ),
            ),
            Case(
                AuthEvent.User.NameKeyboardAction(InputKeyboardAction.Next),
                "profile_creation_user",
                "auth_keyboard_action",
                mapOf(
                    "input_field" to AnalyticsValue.Text("name"),
                    "keyboard_action" to AnalyticsValue.Text("next"),
                ),
            ),
            Case(
                AuthEvent.User.PickerOpened(ProfilePicker.Weight),
                "profile_creation_user",
                "auth_picker_opened",
                mapOf(
                    "input_field" to AnalyticsValue.Text("weight"),
                ),
            ),
            Case(
                AuthEvent.User.PickerConfirmed(ProfilePicker.Height, InputStatus.Valid),
                "profile_creation_user",
                "auth_input_changed",
                mapOf(
                    "input_field" to AnalyticsValue.Text("height"),
                    "input_status" to AnalyticsValue.Text("valid"),
                ),
            ),
            Case(
                AuthEvent.User.Control(ControlAction.Next),
                "profile_creation_user",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("next"),
                ),
            ),
            Case(
                AuthEvent.User.Control(ControlAction.Back),
                "profile_creation_user",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("back"),
                ),
            ),
            Case(
                AuthEvent.Experience.Selected(ProfileExperience.Advanced),
                "profile_creation_experience",
                "auth_experience_selected",
                mapOf(
                    "profile_experience" to AnalyticsValue.Text("advanced"),
                ),
            ),
            Case(
                AuthEvent.Experience.Control(ControlAction.Next),
                "profile_creation_experience",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("next"),
                ),
            ),
            Case(
                AuthEvent.Experience.Control(ControlAction.Back),
                "profile_creation_experience",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("back"),
                ),
            ),
            Case(
                AuthEvent.Muscles.GroupSelected,
                "profile_creation_excluded_muscles",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("group_selected"),
                ),
            ),
            Case(
                AuthEvent.Muscles.SelectionChanged(false, 3),
                "profile_creation_excluded_muscles",
                "auth_selection_changed",
                mapOf(
                    "selected" to AnalyticsValue.Integer(0),
                    "selected_count" to AnalyticsValue.Integer(3),
                ),
            ),
            Case(
                AuthEvent.Muscles.Control(ControlAction.Next),
                "profile_creation_excluded_muscles",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("next"),
                ),
            ),
            Case(
                AuthEvent.Muscles.Control(ControlAction.Back),
                "profile_creation_excluded_muscles",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("back"),
                ),
            ),
            Case(
                AuthEvent.Equipments.GroupSelected,
                "profile_creation_missing_equipments",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("group_selected"),
                ),
            ),
            Case(
                AuthEvent.Equipments.SelectionChanged(true, 4),
                "profile_creation_missing_equipments",
                "auth_selection_changed",
                mapOf(
                    "selected" to AnalyticsValue.Integer(1),
                    "selected_count" to AnalyticsValue.Integer(4),
                ),
            ),
            Case(
                AuthEvent.Equipments.Control(ControlAction.Next),
                "profile_creation_missing_equipments",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("next"),
                ),
            ),
            Case(
                AuthEvent.Equipments.Control(ControlAction.Back),
                "profile_creation_missing_equipments",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("back"),
                ),
            ),
            Case(
                AuthEvent.Completed.Back,
                "profile_creation_completed",
                "auth_control_clicked",
                mapOf(
                    "interaction_action" to AnalyticsValue.Text("back"),
                ),
            ),
        )
        val recorder = RecordingAuthAnalytics()
        val analytics = AuthAnalyticsImpl(recorder)
        cases.forEach { analytics.trackEvent(it.fact) }
        assertEquals(cases.size, recorder.events.size)
        assertEquals(AuthInteractionSchema.all.toSet(), recorder.events.map { it.event.schema }.toSet())
        cases.zip(recorder.events).forEach { (case, captured) ->
            val event = captured.event
            assertEquals(case.schema, event.schema.name)
            assertSame(AuthInteractionSchema.all.single { it.name == case.schema }, event.schema)
            assertEquals(1, event.schema.version)
            assertEquals(
                mapOf("interaction_screen" to AnalyticsValue.Text(case.screen)) + case.parameters,
                event.parameters().associate { it.key.name to it.value },
            )
            assertEquals(event.schema.parameters.size, event.parameters().size)
            event.parameters().zip(event.schema.parameters).forEach { (parameter, key) ->
                assertSame(key, parameter.key)
            }
            assertNull(captured.flow)
            assertNull(captured.operation)
        }
    }

    @Test
    fun everyExplicitCallIsRecordedWithoutHiddenCachesOrScenarioCreation() {
        val recorder = RecordingAuthAnalytics()
        val analytics = AuthAnalyticsImpl(recorder)
        assertTrue(recorder.events.isEmpty())
        repeat(2) {
            analytics.trackEvent(AuthEvent.Login.InputChanged(CredentialField.Email, InputStatus.Valid))
            analytics.trackEvent(AuthEvent.Login.Submit(Method.Email))
            analytics.trackEvent(AuthEvent.User.PickerConfirmed(ProfilePicker.Height, InputStatus.Valid))
        }
        assertEquals(6, recorder.events.size)
        assertTrue(recorder.events.all { it.flow == null && it.operation == null })
    }

    @Test
    fun everyPublicEnumValueKeepsItsStableEncodedPayload() {
        assertEquals(
            listOf("email", "password"),
            encodedValues(CredentialField.entries.map { AuthEvent.Login.InputFocused(it) }, "input_field"),
        )
        assertEquals(
            listOf("height", "weight"),
            encodedValues(ProfilePicker.entries.map { AuthEvent.User.PickerOpened(it) }, "input_field"),
        )
        assertEquals(
            listOf("empty", "invalid", "valid"),
            encodedValues(
                InputStatus.entries.map { AuthEvent.Login.InputChanged(CredentialField.Email, it) },
                "input_status",
            ),
        )
        assertEquals(
            listOf("next", "done"),
            encodedValues(
                InputKeyboardAction.entries.map { AuthEvent.Login.KeyboardAction(CredentialField.Email, it) },
                "keyboard_action",
            ),
        )
        assertEquals(
            listOf("back", "next"),
            encodedValues(ControlAction.entries.map { AuthEvent.Experience.Control(it) }, "interaction_action"),
        )
        assertEquals(
            listOf("beginner", "intermediate", "advanced", "pro"),
            encodedValues(ProfileExperience.entries.map { AuthEvent.Experience.Selected(it) }, "profile_experience"),
        )
        assertEquals(
            listOf("email", "google", "apple"),
            encodedValues(Method.entries.map { AuthEvent.Login.Submit(it) }, "auth_method"),
        )
    }

    @Test
    fun invalidSelectionEncodingCannotPreventFollowingBusinessWork() {
        val recorder = RecordingAuthAnalytics()
        val analytics = AuthAnalyticsImpl(recorder)
        analytics.trackEvent(AuthEvent.Muscles.SelectionChanged(true, -1))
        analytics.trackEvent(AuthEvent.Equipments.SelectionChanged(false, Int.MIN_VALUE))
        assertTrue(recorder.events.isEmpty())
        analytics.trackEvent(AuthEvent.Muscles.SelectionChanged(false, 0))
        assertEquals(1, recorder.events.size)
        assertEquals(AnalyticsValue.Integer(0), recorder.events.single().event.parameters().last().value)
    }

    @Test
    fun reportingFailureDoesNotPreventUserActionsOrWholeBusinessOperations() = runTest {
        val recorder = RecordingAuthAnalytics()
        val observer = object : Analytics by recorder {
            override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
                throw IllegalStateException("private reporting failure")
            }
        }
        val analytics = AuthAnalyticsImpl(observer)
        analytics.trackEvent(AuthEvent.Login.Back)
        analytics.trackEvent(AuthEvent.Splash.Back)
        analytics.trackEvent(AuthEvent.User.Control(ControlAction.Next))
        val result = Any()
        assertSame(result, analytics.trackOperation(AuthOperation.Login(Method.Email)) {
            result
        })
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun fatalReportingErrorsRemainExplicit() {
        val recorder = RecordingAuthAnalytics()
        val original = AssertionError("fatal")
        val observer = object : Analytics by recorder {
            override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
                throw original
            }
        }
        assertSame(original, assertFailsWith<AssertionError> {
            AuthAnalyticsImpl(observer).trackEvent(AuthEvent.Login.Back)
        })
    }

    private fun encodedValues(events: List<AuthEvent>, key: String): List<String?> {
        val recorder = RecordingAuthAnalytics()
        val analytics = AuthAnalyticsImpl(recorder)
        events.forEach(analytics::trackEvent)
        assertEquals(events.size, recorder.events.size)
        return recorder.events.map { it.text(key) }
    }
}
