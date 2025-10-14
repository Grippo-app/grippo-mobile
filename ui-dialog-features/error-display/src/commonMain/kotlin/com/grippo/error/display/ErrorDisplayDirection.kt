package com.grippo.error.display

import com.grippo.core.foundation.models.BaseDirection

public sealed interface ErrorDisplayDirection : BaseDirection {
    public data object Back : ErrorDisplayDirection
}