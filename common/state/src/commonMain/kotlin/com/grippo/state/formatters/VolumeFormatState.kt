package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.kg
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

@Immutable
public data class VolumeFormatState(
    val value: Float
) {

    @Composable
    public fun short(): String {
        val kg = AppTokens.strings.res(Res.string.kg)
        return "${value.short()}$kg"
    }

    private fun Float.short(): String {
        val absValue = abs(this)

        return when {
            absValue >= 1_000_000 -> {
                val rounded = (absValue / 1_000_000).round(1)
                val value =
                    if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
                "$value(M)"
            }

            absValue >= 1_000 -> {
                val rounded = (absValue / 1_000).round(1)
                val value =
                    if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
                "$value(K)"
            }

            else -> this.round(1).toString()
        }
    }

    private fun Float.round(decimals: Int): Float {
        val factor = 10.0.pow(decimals).toFloat()
        return (this * factor).roundToInt() / factor
    }
}