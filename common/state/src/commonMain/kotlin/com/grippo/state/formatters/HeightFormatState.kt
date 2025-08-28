package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class HeightFormatState : FormatState<Int> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Int
    ) : HeightFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Int?
    ) : HeightFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Int? = null
    ) : HeightFormatState()

    public companion object {
        public fun of(display: String): HeightFormatState {
            if (display.isEmpty()) {
                return Empty()
            }

            return try {
                val height = display.toInt()

                when {
                    height == 0 -> Empty()

                    HeightValidator.isValid(height) -> Valid(
                        display = display,
                        value = height
                    )

                    else -> Invalid(
                        display = display,
                        value = height
                    )
                }
            } catch (_: NumberFormatException) {
                Invalid(
                    display = display,
                    value = null
                )
            }
        }

        public fun of(value: Int): HeightFormatState {
            return when {
                value == 0 -> Empty()

                HeightValidator.isValid(value) -> Valid(
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

    private object HeightValidator {
        fun isValid(value: Int): Boolean {
            return value in 100..250
        }
    }
}