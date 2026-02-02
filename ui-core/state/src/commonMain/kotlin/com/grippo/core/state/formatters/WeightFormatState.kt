package com.grippo.core.state.formatters

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
        override val value: Float?
    ) : WeightFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : WeightFormatState()

    public companion object {
        public val WeightLimitation: ClosedFloatingPointRange<Float> = 30.0f..150.0f

        public fun of(display: String): WeightFormatState {
            if (display.isEmpty()) {
                return Empty()
            }

            return try {
                val weight = display.toFloat()

                when {
                    weight == 0f -> Empty()

                    WeightValidator.isValid(weight) -> Valid(
                        display = display,
                        value = weight
                    )

                    else -> Invalid(
                        display = display,
                        value = weight
                    )
                }
            } catch (_: NumberFormatException) {
                Invalid(
                    display = display,
                    value = null
                )
            }
        }

        public fun of(value: Float?): WeightFormatState {
            return when {
                value == null -> Empty()

                value == 0f -> Empty()

                WeightValidator.isValid(value) -> Valid(
                    display = value.toString(),
                    value = value
                )

                else -> Invalid(
                    display = value.toString(),
                    value = value
                )
            }
        }
    }

    private object WeightValidator {
        fun isValid(value: Float): Boolean {
            val withinRange = value in WeightLimitation
            val hasOneDecimal = (value * 10).rem(1f) == 0f
            return withinRange && hasOneDecimal
        }
    }
}