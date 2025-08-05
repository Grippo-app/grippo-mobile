package com.grippo.shared.dialog.content

internal interface DialogContentContract {
    fun onBack(pendingResult: (() -> Unit)?)

    companion object Empty : DialogContentContract {
        override fun onBack(pendingResult: (() -> Unit)?) {}
    }
}