package com.grippo.state.domain.settings

import com.grippo.data.features.api.settings.models.Theme
import com.grippo.state.settings.ThemeState

public fun ThemeState.toDomain(): Theme {
    return when (this) {
        ThemeState.LIGHT -> Theme.LIGHT
        ThemeState.DARK -> Theme.DARK
    }
}