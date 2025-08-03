package com.grippo.shared.dialog.content

import com.grippo.core.models.BaseDirection

internal sealed interface DialogContentDirection : BaseDirection {
    data object Back : DialogContentDirection
}