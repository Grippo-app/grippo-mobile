package com.grippo.state.formatters

import androidx.compose.runtime.Immutable

@Immutable
public sealed class PasswordFormatState(public open val value: String) {
    @Immutable
    public data class Valid(
        override val value: String
    ) : PasswordFormatState(value = value)

    @Immutable
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

private object PasswordValidator {

    fun isValid(value: String): Boolean {
        return value.length > 6
    }
}
