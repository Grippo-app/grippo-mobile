package com.grippo.confirmation

import com.grippo.core.models.BaseDirection

public sealed interface ConfirmationDirection : BaseDirection {
    public data object Confirm : ConfirmationDirection
    public data object Back : ConfirmationDirection
}
