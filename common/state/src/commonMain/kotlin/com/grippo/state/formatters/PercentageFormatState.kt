package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class PercentageFormatState : FormatState<Int> {

    @Immutable
    @Serializable
    public data class Valid(
        override val displayValue: String,
        override val value: Int
    ) : PercentageFormatState() {
        override val isValid: Boolean = true
    }

    @Immutable
    @Serializable
    public data class Invalid(
        override val displayValue: String,
        override val value: Int? = null
    ) : PercentageFormatState() {
        override val isValid: Boolean = false
    }

    public companion object {
        public fun of(displayValue: String): PercentageFormatState {
            return if (displayValue.isEmpty()) {
                Invalid(displayValue)
            } else {
                try {
                    val percentage = displayValue.toInt()
                    if (PercentageValidator.isValid(percentage)) {
                        Valid(displayValue, percentage)
                    } else {
                        Invalid(displayValue, percentage)
                    }
                } catch (e: NumberFormatException) {
                    Invalid(displayValue)
                }
            }
        }

        public fun of(internalValue: Int): PercentageFormatState {
            return if (PercentageValidator.isValid(internalValue)) {
                Valid(internalValue.toString(), internalValue)
            } else {
                Invalid(internalValue.toString(), internalValue)
            }
        }
    }

    @Composable
    public fun short(): String {
        return AppTokens.strings.res(Res.string.percent, value ?: 0)
    }

    private object PercentageValidator {
        fun isValid(value: Int): Boolean {
            return value in 0..100
        }
    }
}