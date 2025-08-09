package com.grippo.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.x

@Immutable
public data class RepetitionsFormatState(val value: Int) {
    @Composable
    public fun short(): String {
        val times = AppTokens.strings.res(Res.string.x)
        return "$times$value"
    }
}