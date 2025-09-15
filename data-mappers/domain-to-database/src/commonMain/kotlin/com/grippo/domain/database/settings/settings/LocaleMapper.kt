package com.grippo.domain.database.settings.settings

import com.grippo.data.features.api.settings.models.Locale
import com.grippo.database.entity.Locale as LocaleEntity

public fun Locale.toEntity(): LocaleEntity {
    return when (this) {
        Locale.EN -> LocaleEntity.EN
        Locale.UA -> LocaleEntity.UA
    }
}