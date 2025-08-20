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
        override val displayValue: String,
        override val value: Int
    ) : RepetitionsFormatState() {
        override val isValid: Boolean = true
    }

    @Immutable
    @Serializable
    public data class Invalid(
        override val displayValue: String,
        override val value: Int? = null
    ) : RepetitionsFormatState() {
        override val isValid: Boolean = false
    }

    public companion object {
        public fun of(displayValue: String): RepetitionsFormatState {
            return if (displayValue.isEmpty()) {
                Invalid(displayValue)
            } else {
                try {
                    val repetitions = displayValue.toInt()
                    if (RepetitionsValidator.isValid(repetitions)) {
                        Valid(displayValue, repetitions)
                    } else {
                        Invalid(displayValue, repetitions)
                    }
                } catch (e: NumberFormatException) {
                    Invalid(displayValue)
                }
            }
        }

        public fun of(internalValue: Int): RepetitionsFormatState {
            return if (RepetitionsValidator.isValid(internalValue)) {
                Valid(internalValue.toString(), internalValue)
            } else {
                Invalid(internalValue.toString(), internalValue)
            }
        }
    }

    @Composable
    public fun short(): String {
        return AppTokens.strings.res(Res.string.x, value ?: 0)
    }

    private object RepetitionsValidator {
        fun isValid(value: Int): Boolean {
            return value in 1..100
        }
    }
}