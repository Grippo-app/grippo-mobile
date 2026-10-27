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
import com.grippo.toolkit.number.utils.formatDecimal
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class VolumeFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : VolumeFormatState(), FormatState.Valid<Float>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : VolumeFormatState(), FormatState.Invalid<Float>

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : VolumeFormatState(), FormatState.Empty<Float>

    public companion object {
        @Composable
        public fun shortTotal(value: Float?): String {
            if (value == null || !value.isFinite()) return "—"
            val number = formatDecimal(
                value = value.toDouble(),
                decimalSeparator = AppTokens.strings.res(Res.string.decimal_separator),
            )
            return "$number${FormatState.UNIT_SPACE}${AppTokens.strings.res(Res.string.kg)}"
        }

        public fun ofTotal(value: Float?): VolumeFormatState = when {
            value == null -> Empty()
            !value.isFinite() || value < 0f -> Invalid(display = value.toString(), value = null)
            else -> Valid(display = formatVolume(value = value, decimalSeparator = ","), value = value)
        }

        private fun formatVolume(value: Float, decimalSeparator: String): String {
            return formatDecimal(value = value.toDouble(), decimalSeparator = decimalSeparator)
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

        public fun of(display: String, allowZero: Boolean = false): VolumeFormatState {
            if (display.isEmpty()) return Empty()

            return try {
                val parsed = display.replace(',', '.').toFloat()
                if (!parsed.isFinite()) {
                    return Invalid(display = display, value = null)
                }
                if (allowZero && parsed < 0f) {
                    return Invalid(display = display, value = parsed)
                }
                val normalized = normalize1dp(parsed)

                when {
                    normalized == 0f && !allowZero -> Empty()

                    VolumeValidator.isValid(normalized) -> Valid(
                        display = display,
                        value = normalized
                    )

                    else -> Invalid(
                        display = display,
                        value = normalized
                    )
                }
            } catch (_: NumberFormatException) {
                Invalid(display = display, value = null)
            }
        }

        public fun of(value: Float?, allowZero: Boolean = false): VolumeFormatState {
            if (value != null && !value.isFinite()) {
                return Invalid(display = value.toString(), value = null)
            }
            if (allowZero && value != null && value < 0f) {
                return Invalid(display = value.toString(), value = value)
            }
            val normalized = value?.let(::normalize1dp)

            return when {
                normalized == null -> Empty()
                normalized == 0f && !allowZero -> Empty()

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
        val separator = AppTokens.strings.res(Res.string.decimal_separator)
        val number = value?.let { formatVolume(value = it, decimalSeparator = separator) }
        return "${number ?: "-"}${FormatState.UNIT_SPACE}$kg"
    }

    @Composable
    public fun shortAnnotated(): AnnotatedString {
        val kg = AppTokens.strings.res(Res.string.kg)
        val tertiary = AppTokens.colors.text.tertiary
        val separator = AppTokens.strings.res(Res.string.decimal_separator)
        val display = value?.let { formatVolume(value = it, decimalSeparator = separator) }
        return buildAnnotatedString {
            if (display != null) {
                append(display)
                append(FormatState.UNIT_SPACE)
                append(kg)
            } else {
                withStyle(SpanStyle(color = tertiary)) {
                    append("-")
                    append(FormatState.UNIT_SPACE)
                    append(kg)
                }
            }
        }
    }

    private object VolumeValidator {
        fun isValid(value: Float): Boolean {
            return value in 0f..1000f
        }
    }
}
