package com.grippo.analytics.authorization.models

import com.grippo.analytics.common.models.ControlAction
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.analytics.common.models.InputStatus

/** Bounded observations only: no credentials, raw input, measurements or selected IDs. */
public sealed interface AuthEvent {
    public sealed interface Splash : AuthEvent {
        public data object Back : Splash
    }

    public sealed interface Login : AuthEvent {
        public data class InputChanged(public val field: CredentialField, public val status: InputStatus) : Login
        public data class InputFocused(public val field: CredentialField) : Login
        public data class InputCleared(public val field: CredentialField) : Login
        public data class KeyboardAction(public val field: CredentialField, public val action: InputKeyboardAction) : Login
        public data class PasswordVisibilityChanged(public val visible: Boolean) : Login
        public data class Submit(public val method: Method) : Login
        public data object Register : Login
        public data object Back : Login
    }

    public sealed interface Registration : AuthEvent {
        public data class InputChanged(public val field: CredentialField, public val status: InputStatus) : Registration
        public data class InputFocused(public val field: CredentialField) : Registration
        public data class InputCleared(public val field: CredentialField) : Registration
        public data class KeyboardAction(public val field: CredentialField, public val action: InputKeyboardAction) : Registration
        public data class PasswordVisibilityChanged(public val visible: Boolean) : Registration
        public data object Submit : Registration
        public data object Back : Registration
    }

    public sealed interface User : AuthEvent {
        public data class NameInputChanged(public val status: InputStatus) : User
        public data object NameInputFocused : User
        public data object NameInputCleared : User
        public data class NameKeyboardAction(public val action: InputKeyboardAction) : User
        public data class PickerOpened(public val field: ProfilePicker) : User
        public data class PickerConfirmed(public val field: ProfilePicker, public val status: InputStatus) : User
        public data class Control(public val action: ControlAction) : User
    }

    public sealed interface Experience : AuthEvent {
        public data class Selected(public val value: ProfileExperience) : Experience
        public data class Control(public val action: ControlAction) : Experience
    }

    public sealed interface Muscles : AuthEvent {
        public data object GroupSelected : Muscles
        public data class SelectionChanged(public val selected: Boolean, public val selectedCount: Int) : Muscles
        public data class Control(public val action: ControlAction) : Muscles
    }

    public sealed interface Equipments : AuthEvent {
        public data object GroupSelected : Equipments
        public data class SelectionChanged(public val selected: Boolean, public val selectedCount: Int) : Equipments
        public data class Control(public val action: ControlAction) : Equipments
    }

    public sealed interface Completed : AuthEvent {
        public data object Back : Completed
    }
}
