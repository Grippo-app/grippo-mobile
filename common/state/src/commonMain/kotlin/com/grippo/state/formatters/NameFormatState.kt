package com.grippo.state.formatters

import androidx.compose.runtime.Immutable

@Immutable
public sealed class NameFormatState(public open val value: String) {
    @Immutable
    public data class Valid(
        override val value: String
    ) : NameFormatState(value = value)

    @Immutable
    public data class Invalid(
        override val value: String
    ) : NameFormatState(value = value)

    public companion object {
        public fun of(value: String): NameFormatState {
            return if (NameValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}

private object NameValidator {
    fun isValid(value: String): Boolean {
        return value.length > 3
    }
}
