package com.grippo.settings.system

import com.grippo.core.models.BaseDirection

internal sealed interface SystemDirection : BaseDirection {
    data object Back : SystemDirection
}