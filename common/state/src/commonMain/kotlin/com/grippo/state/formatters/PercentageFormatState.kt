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
        override val display: String,
        override val value: Int
    ) : PercentageFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Int? = null
    ) : PercentageFormatState()

    public companion object {
        public fun of(display: String): PercentageFormatState {
            return if (display.isEmpty()) {
                Invalid(display)
            } else {
                try {
                    val percentage = display.toInt()
                    if (PercentageValidator.isValid(percentage)) {
                        Valid(display, percentage)
                    } else {
                        Invalid(display, percentage)
                    }
                } catch (_: NumberFormatException) {
                    Invalid(display)
                }
            }
        }

        public fun of(value: Int): PercentageFormatState {
            return if (PercentageValidator.isValid(value)) {
                Valid(value.toString(), value)
            } else {
                Invalid(value.toString(), value)
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value ?: "-"}${percent}"
    }

    private object PercentageValidator {
        fun isValid(value: Int): Boolean {
            return true
        }
    }
}