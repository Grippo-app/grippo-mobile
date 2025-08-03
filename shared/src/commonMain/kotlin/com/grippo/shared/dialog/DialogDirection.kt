package com.grippo.shared.dialog

import com.grippo.core.models.BaseDirection
import com.grippo.dialog.api.DialogConfig

internal sealed interface DialogDirection : BaseDirection {
    // Main frame oriented
    data class Activate(val config: DialogConfig) : DialogDirection
    data class Dismiss(val config: DialogConfig) : DialogDirection

    // Stack oriented
    data class Push(val config: DialogConfig) : DialogDirection
    data object Pop : DialogDirection
}