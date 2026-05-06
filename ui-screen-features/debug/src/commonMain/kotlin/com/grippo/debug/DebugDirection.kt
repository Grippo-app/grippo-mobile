package com.grippo.debug

import com.grippo.core.foundation.models.BaseDirection

public sealed interface DebugDirection : BaseDirection {
    public data object Back : DebugDirection
}