package com.grippo.state.formatters

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
        override val value: Float? = null
    ) : VolumeFormatState()

    public companion object {
        public fun of(display: String): VolumeFormatState {
            return if (display.isEmpty()) {
                Invalid(display)
            } else {
                try {
                    val volume = display.toFloat()
                    if (VolumeValidator.isValid(volume)) {
                        Valid(display, volume)
                    } else {
                        Invalid(display, volume)
                    }
                } catch (_: NumberFormatException) {
                    Invalid(display)
                }
            }
        }

        public fun of(value: Float): VolumeFormatState {
            return if (VolumeValidator.isValid(value)) {
                Valid(value.toString(), value)
            } else {
                Invalid(value.toString(), value)
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