package com.grippo.state.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.theme_dark
import com.grippo.design.resources.provider.theme_light

@Immutable
public enum class ThemeState {
    LIGHT,
    DARK;

    @Composable
    public fun title(): String {
        val r = when (this) {
            LIGHT -> Res.string.theme_light
            DARK -> Res.string.theme_dark
        }
        return AppTokens.strings.res(r)
    }
}