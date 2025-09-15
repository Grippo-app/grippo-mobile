package com.grippo.domain.state.settings

import com.grippo.data.features.api.settings.models.Locale
import com.grippo.state.settings.LocaleState

public fun Locale.toState(): LocaleState {
    return when (this) {
        Locale.EN -> LocaleState.EN
        Locale.UA -> LocaleState.UA
    }
}