package com.grippo.domain.state.settings

import com.grippo.data.features.api.settings.models.Theme
import com.grippo.state.settings.ThemeState

public fun Theme.toState(): ThemeState {
    return when (this) {
        Theme.LIGHT -> ThemeState.LIGHT
        Theme.DARK -> ThemeState.DARK
    }
}