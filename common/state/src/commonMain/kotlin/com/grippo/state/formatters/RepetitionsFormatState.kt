package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.x
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class RepetitionsFormatState : FormatState<Int> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Int
    ) : RepetitionsFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Int? = null
    ) : RepetitionsFormatState()

    public companion object {
        public fun of(display: String): RepetitionsFormatState {
            return if (display.isEmpty()) {
                Invalid(display)
            } else {
                try {
                    val repetitions = display.toInt()
                    if (RepetitionsValidator.isValid(repetitions)) {
                        Valid(display, repetitions)
                    } else {
                        Invalid(display, repetitions)
                    }
                } catch (e: NumberFormatException) {
                    Invalid(display)
                }
            }
        }

        public fun of(value: Int): RepetitionsFormatState {
            return if (RepetitionsValidator.isValid(value)) {
                Valid(value.toString(), value)
            } else {
                Invalid(value.toString(), value)
            }
        }
    }

    @Composable
    public fun short(): String {
        val x = AppTokens.strings.res(Res.string.x)
        return "$x${value ?: "-"}"
    }

    private object RepetitionsValidator {
        fun isValid(value: Int): Boolean {
            return value in 1..100
        }
    }
}