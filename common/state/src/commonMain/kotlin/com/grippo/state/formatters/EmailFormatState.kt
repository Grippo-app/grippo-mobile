package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class EmailFormatState : FormatState<String> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: String
    ) : EmailFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: String? = null
    ) : EmailFormatState()

    public companion object {
        public fun of(value: String): EmailFormatState {
            return if (value.isEmpty()) {
                Invalid(value)
            } else if (EmailValidator.isValid(value)) {
                Valid(value, value)
            } else {
                Invalid(value, value)
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