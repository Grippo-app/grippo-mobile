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
        public fun of(displayValue: String): IntensityFormatState {
            return if (displayValue.isEmpty()) {
                Invalid(displayValue)
            } else {
                try {
                    val intensity = displayValue.toFloat()
                    if (IntensityValidator.isValid(intensity)) {
                        Valid(displayValue, intensity)
                    } else {
                        Invalid(displayValue, intensity)
                    }
                } catch (_: NumberFormatException) {
                    Invalid(displayValue)
                }
            }
        }

        public fun of(internalValue: Float): IntensityFormatState {
            return if (IntensityValidator.isValid(internalValue)) {
                Valid(internalValue.toString(), internalValue)
            } else {
                Invalid(internalValue.toString(), internalValue)
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