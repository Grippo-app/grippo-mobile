package com.grippo.analytics.authorization.internal.encoding

import com.grippo.analytics.authorization.internal.models.AuthAction
import com.grippo.analytics.authorization.internal.models.InputField
import com.grippo.analytics.authorization.internal.models.InteractionAction
import com.grippo.analytics.authorization.internal.models.ProfileAction
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.authorization.models.CredentialField
import com.grippo.analytics.authorization.models.Method
import com.grippo.analytics.authorization.models.ProfileExperience
import com.grippo.analytics.authorization.models.ProfilePicker
import com.grippo.analytics.common.models.ControlAction
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.analytics.common.models.InputStatus

/** Stable wire values belong to encoding; public enums contain only observed facts. */
internal object AuthEncoding {
    fun code(value: AuthScreen): String {
        return when (value) {
            AuthScreen.Splash -> "auth_splash"
            AuthScreen.Onboarding -> "auth_onboarding"
            AuthScreen.Login -> "auth_login"
            AuthScreen.Registration -> "auth_registration"
            AuthScreen.User -> "profile_creation_user"
            AuthScreen.Experience -> "profile_creation_experience"
            AuthScreen.ExcludedMuscles -> "profile_creation_excluded_muscles"
            AuthScreen.MissingEquipments -> "profile_creation_missing_equipments"
            AuthScreen.Completed -> "profile_creation_completed"
        }
    }

    fun code(value: Method): String {
        return when (value) {
            Method.Email -> "email"
            Method.Google -> "google"
            Method.Apple -> "apple"
        }
    }

    fun code(value: ProfileExperience): String {
        return when (value) {
            ProfileExperience.Beginner -> "beginner"
            ProfileExperience.Intermediate -> "intermediate"
            ProfileExperience.Advanced -> "advanced"
            ProfileExperience.Pro -> "pro"
        }
    }

    fun code(value: InputStatus): String {
        return when (value) {
            InputStatus.Empty -> "empty"
            InputStatus.Invalid -> "invalid"
            InputStatus.Valid -> "valid"
        }
    }

    fun code(value: InputKeyboardAction): String {
        return when (value) {
            InputKeyboardAction.Next -> "next"
            InputKeyboardAction.Done -> "done"
        }
    }

    fun code(value: AuthAction): String {
        return when (value) {
            AuthAction.Login -> "login"
            AuthAction.Register -> "register"
        }
    }

    fun code(value: InputField): String {
        return when (value) {
            InputField.Email -> "email"
            InputField.Password -> "password"
            InputField.Name -> "name"
            InputField.Height -> "height"
            InputField.Weight -> "weight"
        }
    }

    fun code(value: InteractionAction): String {
        return when (value) {
            InteractionAction.DismissRequested -> "dismiss_requested"
            InteractionAction.Next -> "next"
            InteractionAction.Register -> "register"
            InteractionAction.GroupSelected -> "group_selected"
        }
    }

    fun code(value: ProfileAction): String {
        return when (value) {
            ProfileAction.Create -> "create"
            ProfileAction.Logout -> "logout"
        }
    }

    fun field(value: CredentialField): InputField {
        return when (value) {
            CredentialField.Email -> InputField.Email
            CredentialField.Password -> InputField.Password
        }
    }

    fun field(value: ProfilePicker): InputField {
        return when (value) {
            ProfilePicker.Height -> InputField.Height
            ProfilePicker.Weight -> InputField.Weight
        }
    }

    fun action(value: ControlAction): InteractionAction {
        return when (value) {
            ControlAction.DismissRequested -> InteractionAction.DismissRequested
            ControlAction.Next -> InteractionAction.Next
        }
    }
}
