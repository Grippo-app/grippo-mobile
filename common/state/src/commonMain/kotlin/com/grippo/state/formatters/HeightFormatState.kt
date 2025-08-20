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
        public fun of(displayValue: String): HeightFormatState {
            return if (displayValue.isEmpty()) {
                Invalid(displayValue)
            } else {
                try {
                    val height = displayValue.toInt()
                    if (HeightValidator.isValid(height)) {
                        Valid(displayValue, height)
                    } else {
                        Invalid(displayValue, height)
                    }
                } catch (e: NumberFormatException) {
                    Invalid(displayValue)
                }
            }
        }

        public fun of(internalValue: Int): HeightFormatState {
            return if (HeightValidator.isValid(internalValue)) {
                Valid(internalValue.toString(), internalValue)
            } else {
                Invalid(internalValue.toString(), internalValue)
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
