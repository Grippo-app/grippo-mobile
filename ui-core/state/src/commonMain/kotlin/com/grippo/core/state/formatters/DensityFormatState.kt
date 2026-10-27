package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.decimal_separator
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.minutes_short
import com.grippo.toolkit.number.utils.formatDecimal
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class DensityFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : DensityFormatState(), FormatState.Valid<Float>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : DensityFormatState(), FormatState.Invalid<Float>

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : DensityFormatState(), FormatState.Empty<Float>

    public companion object {
        @Composable
        public fun shortTotal(value: Float?): String {
            if (value == null || !value.isFinite()) return "—"
            val number = formatDecimal(
                value = value.toDouble(),
                decimalSeparator = AppTokens.strings.res(Res.string.decimal_separator),
            )
            val kg = AppTokens.strings.res(Res.string.kg)
            val minutes = AppTokens.strings.res(Res.string.minutes_short)
            return "$number${FormatState.UNIT_SPACE}$kg\u2060/\u2060$minutes"
        }

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

        public fun of(value: Float?): DensityFormatState {
            if (value != null && !value.isFinite()) {
                return Invalid(display = value.toString(), value = null)
            }
            val normalized = value?.let(::normalize1dp)

            return when {
                normalized == null -> Empty()
                normalized == 0f -> Empty()

                DensityValidator.isValid(normalized) -> Valid(
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
        val minutes = AppTokens.strings.res(Res.string.minutes_short)
        val separator = AppTokens.strings.res(Res.string.decimal_separator)
        val number = value?.let { formatDecimal(value = it.toDouble(), decimalSeparator = separator) }
        return "${number ?: "-"}${FormatState.UNIT_SPACE}$kg/$minutes"
    }

    @Composable
    public fun shortAnnotated(): AnnotatedString {
        val kg = AppTokens.strings.res(Res.string.kg)
        val minutes = AppTokens.strings.res(Res.string.minutes_short)
        val tertiary = AppTokens.colors.text.tertiary
        val separator = AppTokens.strings.res(Res.string.decimal_separator)
        val display = value?.let { formatDecimal(value = it.toDouble(), decimalSeparator = separator) }
        return buildAnnotatedString {
            if (display != null) {
                append(display)
                append(FormatState.UNIT_SPACE)
                append(kg)
                append("/")
                append(minutes)
            } else {
                withStyle(SpanStyle(color = tertiary)) {
                    append("-")
                    append(FormatState.UNIT_SPACE)
                    append(kg)
                    append("/")
                    append(minutes)
                }
            }
        }
    }

    private object DensityValidator {
        fun isValid(value: Float): Boolean {
            return value.isFinite() && value > 0f
        }
    }
}
