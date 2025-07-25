package com.grippo.domain.mapper.settings

import com.grippo.data.features.api.settings.models.ColorMode
import com.grippo.database.entity.ColorMode as ColorModeEntity

public fun ColorMode.toEntity(): ColorModeEntity {
    return when (this) {
        ColorMode.Light -> ColorModeEntity.Light
        ColorMode.Dark -> ColorModeEntity.Dark
    }
}