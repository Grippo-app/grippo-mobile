package com.grippo.settings

import com.grippo.core.models.BaseDirection

public sealed interface SettingsDirection : BaseDirection {
    public data object Close : SettingsDirection
}