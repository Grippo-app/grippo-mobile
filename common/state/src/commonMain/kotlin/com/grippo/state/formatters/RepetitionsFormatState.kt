package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.x
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class RepetitionsFormatState {

    public abstract val value: Int

    @Immutable
    @Serializable
    public data class Valid(
        override val value: Int
    ) : RepetitionsFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val value: Int
    ) : RepetitionsFormatState()

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