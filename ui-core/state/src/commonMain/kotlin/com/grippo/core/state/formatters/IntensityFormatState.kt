package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
@Serializable
public sealed class IntensityFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : IntensityFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : IntensityFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : IntensityFormatState()

    public companion object {

        private const val SCALE: Int = 10 // 10 = 1dp, 100 = 2dp

        private fun scaled(value: Float): Int = (value * SCALE.toFloat()).roundToInt()

        private fun normalize(value: Float): Float {
            val s = scaled(value)
            return s / SCALE.toFloat()
        }

        private fun display(value: Float): String {
            val s = scaled(value)
            val absS = abs(s)
            val intPart = absS / SCALE
            val frac = absS % SCALE
            val sign = if (s < 0) "-" else ""

            val fracStr = when (SCALE) {
                10 -> frac.toString()
                100 -> if (frac < 10) "0$frac" else frac.toString()
                else -> frac.toString()
            }

            return "$sign$intPart.$fracStr"
        }

        public fun of(value: Float): IntensityFormatState {
            if (!value.isFinite()) {
                return Invalid(display = value.toString(), value = null)
            }

            val trimmed = normalize(value)

            return when {
                trimmed == 0f -> Empty()

                IntensityValidator.isValid(trimmed) -> Valid(
                    display = display(trimmed),
                    value = trimmed
                )

                else -> Invalid(
                    display = display(trimmed),
                    value = trimmed
                )
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value?.roundToInt() ?: "-"}${percent}"
    }

    @Immutable
    public enum class Average {
        LOW,
        MEDIUM,
        LARGE
    }

    public fun average(): Average? {
        val v = value ?: return null

        return when {
            v < 20f -> Average.LOW
            v < 40f -> Average.MEDIUM
            else -> Average.LARGE
        }
    }

    private object IntensityValidator {
        fun isValid(value: Float): Boolean {
            return true
        }
    }
}