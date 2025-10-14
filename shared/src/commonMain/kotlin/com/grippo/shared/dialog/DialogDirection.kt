package com.grippo.shared.dialog

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.dialog.api.DialogConfig

internal sealed interface DialogDirection : BaseDirection {
    // Presentation frame oriented
    data class Activate(val config: DialogConfig) : DialogDirection
    data object Dismiss : DialogDirection

    // Stack oriented
    data class Push(val config: DialogConfig) : DialogDirection
    data class Pop(val pendingResult: (() -> Unit)?) : DialogDirection
}