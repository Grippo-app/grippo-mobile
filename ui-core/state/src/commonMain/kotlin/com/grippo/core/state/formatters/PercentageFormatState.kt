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
import com.grippo.design.resources.provider.percent
import com.grippo.toolkit.number.utils.formatDecimal
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class PercentageFormatState : FormatState<Int> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Int
    ) : PercentageFormatState(), FormatState.Valid<Int>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Int?
    ) : PercentageFormatState(), FormatState.Invalid<Int>

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Int? = null
    ) : PercentageFormatState(), FormatState.Empty<Int>

    public companion object {
        @Composable
        public fun shortShare(value: Float): String {
            require(value.isFinite() && value in 0f..100f)

            val unit = AppTokens.strings.res(Res.string.percent)
            val rounded = value.roundToInt()
            return if (value > 0f && rounded == 0) "<1$unit" else "$rounded$unit"
        }

        @Composable
        public fun shortDelta(value: Double): String {
            if (!value.isFinite()) return "—"
            val number = formatDecimal(
                value = value,
                decimalSeparator = AppTokens.strings.res(Res.string.decimal_separator),
            )
            val sign = if (value > 0.0) "+" else ""
            return "$sign$number%"
        }

        public fun of(display: String): PercentageFormatState {
            val raw = display.trim()
            if (raw.isEmpty()) return Empty()

            val normalized = raw.replace(',', '.').toFloatOrNull()?.roundToInt()
                ?: return Invalid(display = display, value = null)

            return when {
                normalized == 0 -> Empty()
                else -> Valid(
                    display = normalized.toString(),
                    value = normalized
                )
            }
        }

        public fun of(value: Int): PercentageFormatState {
            return when {
                value == 0 -> Empty()
                else -> Valid(
                    display = value.toString(),
                    value = value
                )
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value ?: "-"}$percent"
    }

    @Composable
    public fun shortAnnotated(): AnnotatedString {
        val percent = AppTokens.strings.res(Res.string.percent)
        val tertiary = AppTokens.colors.text.tertiary
        val display = value
        return buildAnnotatedString {
            if (display != null) {
                append(display.toString())
                append(percent)
            } else {
                withStyle(SpanStyle(color = tertiary)) {
                    append("-")
                    append(percent)
                }
            }
        }
    }
}
