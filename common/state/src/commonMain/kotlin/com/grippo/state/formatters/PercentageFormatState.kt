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
        override val value: Int?
    ) : PercentageFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Int? = null
    ) : PercentageFormatState()

    public companion object {
        public fun of(display: String): PercentageFormatState {
            if (display.isEmpty()) {
                return Empty()
            }

            return try {
                val percentage = display.toInt()

                when {
                    percentage == 0 -> Empty()

                    PercentageValidator.isValid(percentage) -> Valid(
                        display = display,
                        value = percentage
                    )

                    else -> Invalid(
                        display = display,
                        value = percentage
                    )
                }
            } catch (_: NumberFormatException) {
                Invalid(
                    display = display,
                    value = null
                )
            }
        }

        public fun of(value: Int): PercentageFormatState {
            return when {
                value == 0 -> Empty()

                PercentageValidator.isValid(value) -> Valid(
                    display = value.toString(),
                    value = value
                )

                else -> Invalid(
                    display = value.toString(),
                    value = value
                )
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value ?: "-"}$percent"
    }

    private object PercentageValidator {
        fun isValid(value: Int): Boolean {
            return true
        }
    }
}