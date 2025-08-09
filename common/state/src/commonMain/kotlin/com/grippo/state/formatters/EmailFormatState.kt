package com.grippo.state.formatters

import androidx.compose.runtime.Immutable

@Immutable
public sealed class EmailFormatState(public open val value: String) {
    @Immutable
    public data class Valid(
        override val value: String
    ) : EmailFormatState(value = value)

    @Immutable
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