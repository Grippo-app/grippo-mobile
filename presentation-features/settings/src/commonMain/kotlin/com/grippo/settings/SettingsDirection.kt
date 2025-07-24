package com.grippo.settings

import com.grippo.core.models.BaseDirection

internal sealed interface SettingsDirection : BaseDirection {
    data object Back : SettingsDirection
}