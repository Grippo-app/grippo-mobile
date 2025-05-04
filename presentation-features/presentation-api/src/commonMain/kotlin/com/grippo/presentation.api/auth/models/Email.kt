package com.grippo.presentation.api.auth.models

import androidx.compose.runtime.Immutable
import com.grippo.validation.EmailValidator

@Immutable
public sealed class Email(public open val value: String) {
    public data class Valid(
        override val value: String
    ) : Email(value = value)

    public data class Invalid(
        override val value: String
    ) : Email(value = value)

    public companion object {
        public fun of(value: String): Email {
            return if (EmailValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}