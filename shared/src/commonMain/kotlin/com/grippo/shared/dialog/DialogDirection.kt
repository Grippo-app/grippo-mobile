package com.grippo.shared.dialog

import com.grippo.core.models.BaseDirection
import com.grippo.dialog.api.DialogConfig

internal sealed interface DialogDirection : BaseDirection {
    data class Activate(val config: DialogConfig) : DialogDirection
    data class Dismiss(val config: DialogConfig) : DialogDirection
}