package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import kotlin.math.roundToInt

@Immutable
public sealed class IntensityFormatState(public open val value: Float) {
    @Immutable
    public data class Valid(
        override val value: Float
    ) : IntensityFormatState(value = value)

    @Immutable
    public data class Invalid(
        override val value: Float
    ) : IntensityFormatState(value = value)

    public companion object {
        public fun of(value: Float): IntensityFormatState {
            return if (IntensityValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value.roundToInt()}$percent"
    }
}

private object IntensityValidator {
    fun isValid(value: Float): Boolean {
        return value in 0f..100f
    }
}