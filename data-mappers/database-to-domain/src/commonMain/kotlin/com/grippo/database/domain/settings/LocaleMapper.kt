package com.grippo.database.domain.settings

import com.grippo.data.features.api.settings.models.Locale
import com.grippo.database.entity.Locale as LocaleEntity

public fun LocaleEntity.toDomain(): Locale {
    return when (this) {
        LocaleEntity.EN -> Locale.EN
        LocaleEntity.UA -> Locale.UA
    }
}