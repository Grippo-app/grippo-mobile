package com.grippo.analytics.authorization.internal.schema

import com.grippo.analytics.authorization.internal.encoding.AuthEncoding
import com.grippo.analytics.authorization.internal.models.InputField
import com.grippo.analytics.authorization.internal.models.InteractionAction
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.authorization.models.ProfileExperience
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.analytics.common.models.InputStatus
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

/** Interaction targets are explicit and independent of app-global screen state. */
internal object AuthInteractionSchema {
    val Screen = AnalyticsParameterKey.code<AuthScreen>("interaction_screen", encode = AuthEncoding::code)
    val Action = AnalyticsParameterKey.code<InteractionAction>("interaction_action", encode = AuthEncoding::code)
    val Field = AnalyticsParameterKey.code<InputField>("input_field", encode = AuthEncoding::code)
    val Status = AnalyticsParameterKey.code<InputStatus>("input_status", encode = AuthEncoding::code)
    val KeyboardAction = AnalyticsParameterKey.code<InputKeyboardAction>("keyboard_action", encode = AuthEncoding::code)
    val Visible = AnalyticsParameterKey.flag("password_visible")
    val Experience = AnalyticsParameterKey.code<ProfileExperience>("profile_experience", encode = AuthEncoding::code)
    val Selected = AnalyticsParameterKey.flag("selected")
    val SelectedCount = AnalyticsParameterKey.count("selected_count")

    val Clicked = AnalyticsEventSchema("auth_control_clicked", 1, listOf(Screen, Action))
    val Submitted = AnalyticsEventSchema("auth_submit_clicked", 1, listOf(Screen, AuthOperationSchema.Method))
    val InputChanged = AnalyticsEventSchema("auth_input_changed", 2, listOf(Screen, Field, Status))
    val InputFocused = AnalyticsEventSchema("auth_input_focused", 1, listOf(Screen, Field))
    val InputCleared = AnalyticsEventSchema("auth_input_cleared", 1, listOf(Screen, Field))
    val Keyboard = AnalyticsEventSchema("auth_keyboard_action", 1, listOf(Screen, Field, KeyboardAction))
    val PasswordVisibilityChanged = AnalyticsEventSchema("auth_password_visibility_changed", 1, listOf(Screen, Visible))
    val PickerOpened = AnalyticsEventSchema("auth_picker_opened", 1, listOf(Screen, Field))
    val PickerConfirmed = AnalyticsEventSchema("auth_picker_confirmed", 1, listOf(Screen, Field, Status))
    val ExperienceSelected = AnalyticsEventSchema("auth_experience_selected", 1, listOf(Screen, Experience))
    val SelectionChanged = AnalyticsEventSchema("auth_selection_changed", 1, listOf(Screen, Selected, SelectedCount))

    val all = listOf(
        Clicked,
        Submitted,
        InputChanged,
        InputFocused,
        InputCleared,
        Keyboard,
        PasswordVisibilityChanged,
        PickerOpened,
        PickerConfirmed,
        ExperienceSelected,
        SelectionChanged,
    )
}
