package com.grippo.core.state.formatters

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
        override val value: Int?
    ) : RepetitionsFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Int? = null
    ) : RepetitionsFormatState()

    public companion object {
        public fun of(display: String): RepetitionsFormatState {
            if (display.isEmpty()) {
                return Empty()
            }

            return try {
                val n = display.toInt()

                when {
                    n == 0 -> Empty()

                    RepetitionsValidator.isValid(n) -> Valid(
                        display = display,
                        value = n
                    )

                    else -> Invalid(
                        display = display,
                        value = n
                    )
                }
            } catch (_: NumberFormatException) {
                Invalid(
                    display = display,
                    value = null
                )
            }
        }

        public fun of(value: Int): RepetitionsFormatState {
            return when {
                value == 0 -> Empty()

                RepetitionsValidator.isValid(value) -> Valid(
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
        val x = AppTokens.strings.res(Res.string.x)
        return "$x${value ?: "-"}"
    }

    private object RepetitionsValidator {
        fun isValid(value: Int): Boolean = value in 1..100
    }
}