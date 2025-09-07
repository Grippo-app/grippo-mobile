package com.grippo.domain.database.settings.settings

import com.grippo.data.features.api.settings.models.Theme
import com.grippo.database.entity.Theme as ThemeEntity

public fun Theme.toEntity(): ThemeEntity {
    return when (this) {
        Theme.LIGHT -> ThemeEntity.LIGHT
        Theme.DARK -> ThemeEntity.DARK
    }
}