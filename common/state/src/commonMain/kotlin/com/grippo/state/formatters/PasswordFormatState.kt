package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class PasswordFormatState : FormatState<String> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: String
    ) : PasswordFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: String? = null
    ) : PasswordFormatState()

    public companion object {
        public fun of(display: String): PasswordFormatState {
            return if (display.isEmpty()) {
                Invalid(display)
            } else if (PasswordValidator.isValid(display)) {
                Valid(display, display)
            } else {
                Invalid(display, display)
            }
        }
    }

    private object PasswordValidator {
        fun isValid(value: String): Boolean {
            return value.length > 6
        }
    }
}