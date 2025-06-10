package com.grippo.shared.dialog

import com.grippo.dialog.api.DialogConfig

internal interface DialogContract {

    fun dismiss()
    fun dismiss(pendingResult: (() -> Unit)?)
    fun release(config: DialogConfig)

    companion object Empty : DialogContract {
        override fun dismiss() {}
        override fun dismiss(pendingResult: (() -> Unit)?) {}
        override fun release(config: DialogConfig) {}
    }
}