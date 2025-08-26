package com.grippo.state.settings

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.theme_dark
import com.grippo.design.resources.provider.theme_light
import com.grippo.state.formatters.UiText

@Immutable
public enum class ThemeState {
    LIGHT,
    DARK;

    public fun title(): UiText {
        val r = when (this) {
            LIGHT -> Res.string.theme_light
            DARK -> Res.string.theme_dark
        }
        return UiText.Res(r)
    }
}