package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable
import com.grippo.dialog.api.DialogConfig

@Immutable
internal interface DialogContract {
    fun onClose()
    fun onDismiss(pendingResult: (() -> Unit)?)
    fun onRelease(config: DialogConfig)

    @Immutable
    companion object Empty : DialogContract {
        override fun onClose() {}
        override fun onDismiss(pendingResult: (() -> Unit)?) {}
        override fun onRelease(config: DialogConfig) {}
    }
}