package com.grippo.database.domain.settings

import com.grippo.data.features.api.settings.models.Settings
import com.grippo.database.entity.SettingsEntity

public fun SettingsEntity.toDomain(): Settings {
    return Settings(
        theme = theme.toDomain(),
        locale = locale.toDomain()
    )
}