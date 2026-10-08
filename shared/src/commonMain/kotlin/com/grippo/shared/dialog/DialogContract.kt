package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable

@Immutable
internal interface DialogContract {
    fun onClose()
    fun onDismiss(pendingResult: (() -> Unit)?)
    fun onRelease(session: DialogSession)

    @Immutable
    companion object Empty : DialogContract {
        override fun onClose() {}
        override fun onDismiss(pendingResult: (() -> Unit)?) {}
        override fun onRelease(session: DialogSession) {}
    }
}
