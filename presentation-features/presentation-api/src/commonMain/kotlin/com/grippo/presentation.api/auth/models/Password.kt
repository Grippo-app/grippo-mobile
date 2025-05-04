package com.grippo.presentation.api.auth.models

import androidx.compose.runtime.Immutable
import com.grippo.validation.PasswordValidator

@Immutable
public sealed class Password(public open val value: String) {
    public data class Valid(
        override val value: String
    ) : Password(value = value)

    public data class Invalid(
        override val value: String
    ) : Password(value = value)

    public companion object {
        public fun of(value: String): Password {
            return if (PasswordValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}