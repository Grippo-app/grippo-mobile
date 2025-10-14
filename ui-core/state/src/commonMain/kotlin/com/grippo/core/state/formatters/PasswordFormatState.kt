package com.grippo.core.state.formatters

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
        override val value: String?
    ) : PasswordFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: String? = null
    ) : PasswordFormatState()

    public companion object {
        public fun of(display: String): PasswordFormatState {
            if (display.isEmpty()) {
                return Empty()
            }

            return if (PasswordValidator.isValid(display)) {
                Valid(display = display, value = display)
            } else {
                Invalid(display = display, value = display)
            }
        }
    }

    private object PasswordValidator {
        fun isValid(value: String): Boolean {
            return value.length > 6
        }
    }
}