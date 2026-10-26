package com.grippo.state.domain.language

import com.grippo.core.state.settings.LanguageEnumState
import com.grippo.data.features.api.local.settings.models.Language

public fun LanguageEnumState.toDomain(): Language {
    return when (this) {
        LanguageEnumState.ENGLISH -> Language.ENGLISH
        LanguageEnumState.UKRAINIAN -> Language.UKRAINIAN
        LanguageEnumState.RUSSIAN -> Language.RUSSIAN
        LanguageEnumState.PORTUGUESE_BRAZIL -> Language.PORTUGUESE_BRAZIL
        LanguageEnumState.PORTUGUESE_PORTUGAL -> Language.PORTUGUESE_PORTUGAL
    }
}
