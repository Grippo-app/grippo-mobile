package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Immutable
@Serializable
public sealed class IntensityFormatState : FormatState<Float> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: Float
    ) : IntensityFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: Float?
    ) : IntensityFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: Float? = null
    ) : IntensityFormatState()

    public companion object {
        public fun of(value: Float): IntensityFormatState {
            if (!value.isFinite()) {
                return Invalid(
                    display = value.toString(),
                    value = null
                )
            }

            val trimmed = kotlin.math.round(value * 100f) / 100f

            return when {
                trimmed == 0f -> Empty()

                IntensityValidator.isValid(trimmed) -> Valid(
                    display = value.toString(),
                    value = trimmed
                )

                else -> Invalid(
                    display = value.toString(),
                    value = trimmed
                )
            }
        }
    }

    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value?.roundToInt() ?: "-"}${percent}"
    }

    @Immutable
    public enum class Average {
        LOW,
        MEDIUM,
        LARGE
    }

    public fun average(): Average? {
        val v = value ?: return null

        return when {
            v < 20f -> Average.LOW
            v < 40f -> Average.MEDIUM
            else -> Average.LARGE
        }
    }

    private object IntensityValidator {
        fun isValid(value: Float): Boolean {
            return true
        }
    }
}