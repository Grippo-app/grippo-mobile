package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class NameFormatState : FormatState<String> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: String
    ) : NameFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: String? = null
    ) : NameFormatState()

    public companion object {
        public fun of(display: String): NameFormatState {
            return if (display.isEmpty()) {
                Invalid(display)
            } else if (NameValidator.isValid(display)) {
                Valid(display, display)
            } else {
                Invalid(display, display)
            }
        }
    }

    private object NameValidator {
        fun isValid(value: String): Boolean {
            return value.length > 3
        }
    }
}