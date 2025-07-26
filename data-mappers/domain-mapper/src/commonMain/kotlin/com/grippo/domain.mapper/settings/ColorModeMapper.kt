package com.grippo.domain.mapper.settings

import com.grippo.data.features.api.settings.models.Theme
import com.grippo.presentation.api.settings.models.ThemeState
import com.grippo.database.entity.Theme as ThemeEntity

public fun Theme.toEntity(): ThemeEntity {
    return when (this) {
        Theme.LIGHT -> ThemeEntity.LIGHT
        Theme.DARK -> ThemeEntity.DARK
    }
}

public fun Theme.toState(): ThemeState {
    return when (this) {
        Theme.LIGHT -> ThemeState.LIGHT
        Theme.DARK -> ThemeState.DARK
    }
}