package com.grippo.state.auth

import androidx.compose.runtime.Immutable
import com.grippo.validation.EmailValidator

@Immutable
public sealed class EmailFormatState(public open val value: String) {
    public data class Valid(
        override val value: String
    ) : EmailFormatState(value = value)

    public data class Invalid(
        override val value: String
    ) : EmailFormatState(value = value)

    public companion object {
        public fun of(value: String): EmailFormatState {
            return if (EmailValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}