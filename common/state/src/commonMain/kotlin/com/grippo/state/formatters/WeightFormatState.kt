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
        public fun of(display: String): WeightFormatState {
            return if (display.isEmpty()) {
                Invalid(display)
            } else {
                try {
                    val weight = display.toFloat()
                    if (WeightValidator.isValid(weight)) {
                        Valid(display, weight)
                    } else {
                        Invalid(display, weight)
                    }
                } catch (_: NumberFormatException) {
                    Invalid(display)
                }
            }
        }

        public fun of(value: Float): WeightFormatState {
            return if (WeightValidator.isValid(value)) {
                Valid(value.toString(), value)
            } else {
                Invalid(value.toString(), value)
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