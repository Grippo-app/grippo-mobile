package com.grippo.database.mapper.settings

import com.grippo.data.features.api.settings.models.ColorMode
import com.grippo.database.entity.ColorMode as ColorModeEntity

public fun ColorModeEntity.toDomain(): ColorMode {
    return when (this) {
        ColorModeEntity.Light -> ColorMode.Light
        ColorModeEntity.Dark -> ColorMode.Dark
    }
}