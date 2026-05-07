package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
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
    ) : IntensityFormatState(), FormatState.Valid<Float>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : IntensityFormatState(), FormatState.Invalid<Float>

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : IntensityFormatState(), FormatState.Empty<Float>

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

        public fun of(value: Float): IntensityFormatState {
            if (!value.isFinite()) {
                return Invalid(display = value.toString(), value = null)
            }

            val normalized = normalize1dp(value)

            return when {
                normalized == 0f -> Empty()
                else -> Valid(
                    display = display1dp(normalized),
                    value = normalized
                )
            }
        }
    }

    @Composable
    public fun short(): String {
        val kg = AppTokens.strings.res(Res.string.kg)
        return "${value?.roundToInt() ?: "-"}$kg"
    }

    @Composable
    public fun shortAnnotated(): AnnotatedString {
        val kg = AppTokens.strings.res(Res.string.kg)
        val tertiary = AppTokens.colors.text.tertiary
        val display = value?.roundToInt()
        return buildAnnotatedString {
            if (display != null) {
                append(display.toString())
                append(kg)
            } else {
                withStyle(SpanStyle(color = tertiary)) {
                    append("-")
                    append(kg)
                }
            }
        }
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
}
