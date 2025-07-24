package com.grippo.shared.dialog

import com.grippo.dialog.api.DialogConfig

internal interface DialogContract {
    fun dismiss(pendingResult: (() -> Unit)?)
    fun back()
    fun release(config: DialogConfig)

    companion object Empty : DialogContract {
        override fun dismiss(pendingResult: (() -> Unit)?) {}
        override fun back() {}
        override fun release(config: DialogConfig) {}
    }
}