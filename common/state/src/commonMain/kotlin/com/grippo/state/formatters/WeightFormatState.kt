package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class WeightFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : WeightFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float? = null
    ) : WeightFormatState()

    public companion object {
        public fun of(displayValue: String): WeightFormatState {
            return if (displayValue.isEmpty()) {
                Invalid(displayValue)
            } else {
                try {
                    val weight = displayValue.toFloat()
                    if (WeightValidator.isValid(weight)) {
                        Valid(displayValue, weight)
                    } else {
                        Invalid(displayValue, weight)
                    }
                } catch (_: NumberFormatException) {
                    Invalid(displayValue)
                }
            }
        }

        public fun of(internalValue: Float): WeightFormatState {
            return if (WeightValidator.isValid(internalValue)) {
                Valid(internalValue.toString(), internalValue)
            } else {
                Invalid(internalValue.toString(), internalValue)
            }
        }
    }

    private object WeightValidator {
        fun isValid(value: Float): Boolean {
            val withinRange = value in 30.0f..150.0f
            val hasOneDecimal = (value * 10).rem(1f) == 0f
            return withinRange && hasOneDecimal
        }
    }
}