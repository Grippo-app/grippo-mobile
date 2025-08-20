package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class PasswordFormatState : FormatState<String> {

    @Immutable
    @Serializable
    public data class Valid(
        override val displayValue: String,
        override val value: String
    ) : PasswordFormatState() {
        override val isValid: Boolean = true
    }

    @Immutable
    @Serializable
    public data class Invalid(
        override val displayValue: String,
        override val value: String? = null
    ) : PasswordFormatState() {
        override val isValid: Boolean = false
    }

    public companion object {
        public fun of(displayValue: String): PasswordFormatState {
            return if (displayValue.isEmpty()) {
                Invalid(displayValue)
            } else if (PasswordValidator.isValid(displayValue)) {
                Valid(displayValue, displayValue)
            } else {
                Invalid(displayValue, displayValue)
            }
        }
    }

    private object PasswordValidator {
        fun isValid(value: String): Boolean {
            return value.length > 6
        }
    }
}