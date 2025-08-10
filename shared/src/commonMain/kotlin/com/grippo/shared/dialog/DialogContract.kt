package com.grippo.shared.dialog

import com.grippo.dialog.api.DialogConfig

internal interface DialogContract {
    fun onClose()
    fun onDismiss(pendingResult: (() -> Unit)?)
    fun onRelease(config: DialogConfig)

    companion object Empty : DialogContract {
        override fun onClose() {}
        override fun onDismiss(pendingResult: (() -> Unit)?) {}
        override fun onRelease(config: DialogConfig) {}
    }
}