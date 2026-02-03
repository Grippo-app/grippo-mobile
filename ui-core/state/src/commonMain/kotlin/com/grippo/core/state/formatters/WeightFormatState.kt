package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

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

        private fun tenths(value: Float): Int = (value * 10f).roundToInt()

        private fun normalize1dp(value: Float): Float {
            val t = tenths(value)
            return t / 10f
        }

        private fun display1dp(value: Float): String {
            val t = tenths(value)
            val absT = abs(t)
            val intPart = absT / 10
            val frac = absT % 10
            val sign = if (t < 0) "-" else ""
            return "$sign$intPart.$frac"
        }

        public fun of(display: String): WeightFormatState {
            if (display.isEmpty()) return Empty()

            return try {
                val parsed = display.replace(',', '.').toFloat()
                val normalized = normalize1dp(parsed)

                when {
                    normalized == 0f -> Empty()

                    WeightValidator.isValid(normalized) -> Valid(
                        display = display1dp(normalized),
                        value = normalized
                    )

                    else -> Invalid(
                        display = display1dp(normalized),
                        value = normalized
                    )
                }
            } catch (_: NumberFormatException) {
                Invalid(display = display, value = null)
            }
        }

        public fun of(value: Float?): WeightFormatState {
            val normalized = value?.let(::normalize1dp)

            return when {
                normalized == null -> Empty()
                normalized == 0f -> Empty()

                WeightValidator.isValid(normalized) -> Valid(
                    display = display1dp(normalized),
                    value = normalized
                )

                else -> Invalid(
                    display = display1dp(normalized),
                    value = normalized
                )
            }
        }
    }

    private object WeightValidator {
        fun isValid(value: Float): Boolean {
            val withinRange = value in WeightLimitation
            val t = (value * 10f).roundToInt()
            val normalized = t / 10f
            val hasOneDecimal = abs(value - normalized) < 0.0001f
            return withinRange && hasOneDecimal
        }
    }
}