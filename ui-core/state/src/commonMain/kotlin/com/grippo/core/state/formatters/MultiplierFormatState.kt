package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
@Serializable
public sealed class MultiplierFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : MultiplierFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : MultiplierFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : MultiplierFormatState()

    public companion object {

        private fun hundredths(value: Float): Int = (value * 100f).roundToInt()

        private fun normalize2dp(value: Float): Float {
            val h = hundredths(value)
            return h / 100f
        }

        private fun display2dp(value: Float): String {
            val h = hundredths(value)
            val absH = abs(h)
            val intPart = absH / 100
            val frac = absH % 100
            val sign = if (h < 0) "-" else ""
            val fracStr = if (frac < 10) "0$frac" else frac.toString()
            return "$sign$intPart.$fracStr"
        }

        public fun of(value: Float?): MultiplierFormatState {
            val normalized = value?.let(::normalize2dp)

            return when {
                normalized == null -> Empty()
                normalized == 0f -> Empty()

                MultiplierValidator.isValid(normalized) -> Valid(
                    display = display2dp(normalized),
                    value = normalized
                )

                else -> Invalid(
                    display = display2dp(normalized),
                    value = normalized
                )
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = (value ?: 0f) * 100f
        return "${percent.roundToInt()}%"
    }

    private object MultiplierValidator {
        fun isValid(value: Float): Boolean {
            return value in 0.05f..2.0f
        }
    }
}