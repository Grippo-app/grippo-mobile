package com.grippo.shared.dialog.content

import com.grippo.core.models.BaseDirection

internal sealed interface DialogContentDirection : BaseDirection {
    data class Back(val pendingResult: (() -> Unit)? = null) : DialogContentDirection
}