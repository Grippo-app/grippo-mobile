package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.weight_input_hint
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
@Serializable
public sealed class VolumeFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : VolumeFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : VolumeFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : VolumeFormatState()

    public companion object {
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

        public fun of(display: String): VolumeFormatState {
            if (display.isEmpty()) return Empty()

            return try {
                val parsed = display.replace(',', '.').toFloat()
                val normalized = normalize1dp(parsed)

                when {
                    normalized == 0f -> Empty()

                    VolumeValidator.isValid(normalized) -> Valid(
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

        public fun of(value: Float?): VolumeFormatState {
            val normalized = value?.let(::normalize1dp)

            return when {
                normalized == null -> Empty()
                normalized == 0f -> Empty()

                VolumeValidator.isValid(normalized) -> Valid(
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

    @Composable
    public fun short(): String {
        val kg = AppTokens.strings.res(Res.string.kg)
        return "${value?.short() ?: "-"}$kg"
    }

    private fun Float.short(): String {
        return when {
            this == 0f -> "0"
            abs(this) < 1f -> ((this * 10).roundToInt() / 10.0f).toString()
            abs(this) < 10f -> ((this * 10).roundToInt() / 10.0f).toString()
            abs(this) < 100f -> this.roundToInt().toString()
            else -> {
                val value = this.roundToInt()
                val isNegative = value < 0
                val digits = abs(value).toString()

                val grouped = digits
                    .reversed()
                    .chunked(3)
                    .joinToString(" ")
                    .reversed()

                if (isNegative) "-$grouped" else grouped
            }
        }
    }

    private object VolumeValidator {
        fun isValid(value: Float): Boolean {
            return value in 0f..1000f
        }
    }

    @Composable
    public fun hint(): String {
        return AppTokens.strings.res(Res.string.weight_input_hint)
    }
}