package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

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

        private fun normalize(value: Float): Int = value.roundToInt()

        public fun of(display: String): PercentageFormatState {
            val raw = display.trim()
            if (raw.isEmpty()) return Empty()

            val parsed: Float? = raw.replace(',', '.').toFloatOrNull()
            val normalized: Int? = parsed?.let(::normalize) ?: raw.toIntOrNull()

            return when {
                normalized == null -> Invalid(display = display, value = null)
                normalized == 0 -> Empty()

                PercentageValidator.isValid(normalized) -> Valid(
                    display = normalized.toString(),
                    value = normalized
                )

                else -> Invalid(
                    display = normalized.toString(),
                    value = normalized
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