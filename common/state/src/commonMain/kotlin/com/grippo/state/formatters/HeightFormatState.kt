package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class HeightFormatState : FormatState<Int> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Int
    ) : HeightFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Int? = null
    ) : HeightFormatState()

    public companion object {
        public fun of(display: String): HeightFormatState {
            return if (display.isEmpty()) {
                Invalid(display)
            } else {
                try {
                    val height = display.toInt()
                    if (HeightValidator.isValid(height)) {
                        Valid(display, height)
                    } else {
                        Invalid(display, height)
                    }
                } catch (_: NumberFormatException) {
                    Invalid(display)
                }
            }
        }

        public fun of(value: Int): HeightFormatState {
            return if (HeightValidator.isValid(value)) {
                Valid(value.toString(), value)
            } else {
                Invalid(value.toString(), value)
            }
        }
    }

    private object HeightValidator {
        fun isValid(value: Int): Boolean {
            val withinRange = value in 100..250
            return withinRange
        }
    }
}
