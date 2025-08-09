package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.percent
import kotlin.math.roundToInt

@Immutable
public data class IntensityFormatState(val value: Float) {
    @Composable
    public fun short(): String {
        val percent = AppTokens.strings.res(Res.string.percent)
        return "${value.roundToInt()}$percent"
    }
}