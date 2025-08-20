package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class NameFormatState : FormatState<String> {

    @Immutable
    @Serializable
    public data class Valid(
        override val displayValue: String,
        override val value: String
    ) : NameFormatState() {
        override val isValid: Boolean = true
    }

    @Immutable
    @Serializable
    public data class Invalid(
        override val displayValue: String,
        override val value: String? = null
    ) : NameFormatState() {
        override val isValid: Boolean = false
    }

    public companion object {
        public fun of(displayValue: String): NameFormatState {
            return if (displayValue.isEmpty()) {
                Invalid(displayValue)
            } else if (NameValidator.isValid(displayValue)) {
                Valid(displayValue, displayValue)
            } else {
                Invalid(displayValue, displayValue)
            }
        }
    }

    private object NameValidator {
        fun isValid(value: String): Boolean {
            return value.length > 3
        }
    }
}