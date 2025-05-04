package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable
import com.grippo.dialog.api.DialogEvent

@Immutable
internal data class DialogState(
    val event: DialogEvent? = null
)