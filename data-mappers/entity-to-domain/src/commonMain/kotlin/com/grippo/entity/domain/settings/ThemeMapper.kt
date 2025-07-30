package com.grippo.entity.domain.settings

import com.grippo.data.features.api.settings.models.Theme
import com.grippo.database.entity.Theme as ThemeEntity

public fun ThemeEntity.toDomain(): Theme {
    return when (this) {
        ThemeEntity.LIGHT -> Theme.LIGHT
        ThemeEntity.DARK -> Theme.DARK
    }
}