package com.grippo.presentation.api.auth.models

import androidx.compose.runtime.Immutable
import com.grippo.validation.PasswordValidator

@Immutable
public sealed class PasswordState(public open val value: String) {
    public data class Valid(
        override val value: String
    ) : PasswordState(value = value)

    public data class Invalid(
        override val value: String
    ) : PasswordState(value = value)

    public companion object {
        public fun of(value: String): PasswordState {
            return if (PasswordValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}