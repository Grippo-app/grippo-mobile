package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import kotlinx.serialization.Serializable
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
        override val value: Float? = null
    ) : IntensityFormatState()

    public companion object {
        public fun of(display: String): IntensityFormatState {
            if (display.isEmpty()) return Invalid(display)

            val parsed = display.replace(',', '.').toFloatOrNull() ?: return Invalid(display)
            if (!parsed.isFinite()) return Invalid(display)

            val trimmed = kotlin.math.round(parsed * 100f) / 100f

            return if (IntensityValidator.isValid(trimmed)) {
                Valid(display = display, value = trimmed)
            } else {
                Invalid(display = display, value = trimmed)
            }
        }

        public fun of(value: Float): IntensityFormatState {
            if (!value.isFinite()) return Invalid(value.toString(), null)

            val trimmed = kotlin.math.round(value * 100f) / 100f

            return if (IntensityValidator.isValid(trimmed)) {
                Valid(display = value.toString(), value = trimmed)
            } else {
                Invalid(display = value.toString(), value = trimmed)
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value?.roundToInt() ?: "-"}${percent}"
    }

    private object IntensityValidator {
        fun isValid(value: Float): Boolean {
            return true
        }
    }
}