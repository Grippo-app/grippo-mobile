package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
@Serializable
public sealed class VolumeFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : VolumeFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : VolumeFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : VolumeFormatState()

    public companion object {
        public fun of(display: String): VolumeFormatState {
            if (display.isEmpty()) {
                return Empty()
            }

            return try {
                val volume = display.toFloat()

                when {
                    volume == 0f -> Empty()

                    VolumeValidator.isValid(volume) -> Valid(
                        display = display,
                        value = volume
                    )

                    else -> Invalid(
                        display = display,
                        value = volume
                    )
                }
            } catch (_: NumberFormatException) {
                Invalid(
                    display = display,
                    value = null
                )
            }
        }

        public fun of(value: Float): VolumeFormatState {
            return when {
                value == 0f -> Empty()

                VolumeValidator.isValid(value) -> Valid(
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
        val kg = AppTokens.strings.res(Res.string.kg)
        return "${value?.short() ?: "-"}$kg"
    }

    private fun Float.short(): String {
        return when {
            this == 0f -> "0"
            abs(this) < 1f -> ((this * 10).roundToInt() / 10.0f).toString()
            abs(this) < 10f -> ((this * 10).roundToInt() / 10.0f).toString()
            abs(this) < 100f -> this.roundToInt().toString()
            else -> this.roundToInt().toString()
        }
    }

    private object VolumeValidator {
        fun isValid(value: Float): Boolean {
            return value in 0f..1000f
        }
    }
}