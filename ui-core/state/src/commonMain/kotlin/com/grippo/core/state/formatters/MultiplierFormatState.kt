package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class MultiplierFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : MultiplierFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : MultiplierFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : MultiplierFormatState()

    public companion object {
        public fun of(value: Float?): MultiplierFormatState {
            return when {
                value == null -> Empty()

                value == 0f -> Empty()

                MultiplierValidator.isValid(value) -> Valid(
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
        return "${(value ?: 0f) * 100f}%"
    }

    private object MultiplierValidator {
        fun isValid(value: Float): Boolean {
            return value in 0.05f..2.0f
        }
    }
}