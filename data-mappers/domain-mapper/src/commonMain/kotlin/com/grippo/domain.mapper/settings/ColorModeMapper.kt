package com.grippo.domain.mapper.settings

import com.grippo.data.features.api.settings.models.ColorMode
import com.grippo.presentation.api.settings.models.ColorModeState
import com.grippo.database.entity.ColorMode as ColorModeEntity

public fun ColorMode.toEntity(): ColorModeEntity {
    return when (this) {
        ColorMode.LIGHT -> ColorModeEntity.LIGHT
        ColorMode.DARK -> ColorModeEntity.DARK
    }
}

public fun ColorMode.toState(): ColorModeState {
    return when (this) {
        ColorMode.LIGHT -> ColorModeState.LIGHT
        ColorMode.DARK -> ColorModeState.DARK
    }
}