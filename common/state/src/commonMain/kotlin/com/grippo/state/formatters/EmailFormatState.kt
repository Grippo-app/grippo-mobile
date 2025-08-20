package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class EmailFormatState : FormatState<String> {

    @Immutable
    @Serializable
    public data class Valid(
        override val displayValue: String,
        override val value: String
    ) : EmailFormatState() {
        override val isValid: Boolean = true
    }

    @Immutable
    @Serializable
    public data class Invalid(
        override val displayValue: String,
        override val value: String? = null
    ) : EmailFormatState() {
        override val isValid: Boolean = false
    }

    public companion object {
        public fun of(displayValue: String): EmailFormatState {
            return if (displayValue.isEmpty()) {
                Invalid(displayValue)
            } else if (EmailValidator.isValid(displayValue)) {
                Valid(displayValue, displayValue)
            } else {
                Invalid(displayValue, displayValue)
            }
        }
    }

    private object EmailValidator {
        private val emailAddressRegex = Regex(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+",
        )

        fun isValid(value: String): Boolean {
            return value.matches(emailAddressRegex)
        }
    }
}