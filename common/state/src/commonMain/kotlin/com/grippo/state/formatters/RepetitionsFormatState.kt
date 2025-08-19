package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.x

@Immutable
public sealed class RepetitionsFormatState(public open val value: Int) {
    @Immutable
    public data class Valid(
        override val value: Int
    ) : RepetitionsFormatState(value = value)

    @Immutable
    public data class Invalid(
        override val value: Int
    ) : RepetitionsFormatState(value = value)

    public companion object {
        public fun of(value: Int): RepetitionsFormatState {
            return if (RepetitionsValidator.isValid(value)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }

    @Composable
    public fun short(): String {
        val times = AppTokens.strings.res(Res.string.x)
        return "$times$value"
    }
}

private object RepetitionsValidator {
    fun isValid(value: Int): Boolean {
        return value > 0
    }
}