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
            return if (display.isEmpty()) {
                Invalid(display)
            } else {
                try {
                    val intensity = display.toFloat()
                    if (IntensityValidator.isValid(intensity)) {
                        Valid(display, intensity)
                    } else {
                        Invalid(display, intensity)
                    }
                } catch (_: NumberFormatException) {
                    Invalid(display)
                }
            }
        }

        public fun of(value: Float): IntensityFormatState {
            return if (IntensityValidator.isValid(value)) {
                Valid(value.toString(), value)
            } else {
                Invalid(value.toString(), value)
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