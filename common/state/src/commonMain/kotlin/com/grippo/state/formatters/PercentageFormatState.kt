package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent

@Immutable
public sealed class PercentageFormatState(public open val value: Int) {
    @Immutable
    public data class Valid(
        override val value: Int
    ) : PercentageFormatState(value = value)

    @Immutable
    public data class Invalid(
        override val value: Int
    ) : PercentageFormatState(value = value)

    public companion object {
        public fun of(value: Int): PercentageFormatState {
            return if (PercentageValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value}$percent"
    }
}

private object PercentageValidator {
    fun isValid(value: Int): Boolean {
        return value in 0..100
    }
}