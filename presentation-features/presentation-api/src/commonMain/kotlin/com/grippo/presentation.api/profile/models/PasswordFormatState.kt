package com.grippo.presentation.api.profile.models

import androidx.compose.runtime.Immutable
import com.grippo.validation.PasswordValidator

@Immutable
public sealed class PasswordFormatState(public open val value: String) {
    public data class Valid(
        override val value: String
    ) : PasswordFormatState(value = value)

    public data class Invalid(
        override val value: String
    ) : PasswordFormatState(value = value)

    public companion object {
        public fun of(value: String): PasswordFormatState {
            return if (PasswordValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}