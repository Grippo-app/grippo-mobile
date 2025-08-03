package com.grippo.shared.dialog.content

internal interface DialogContentContract {
    fun onBack()

    companion object Empty : DialogContentContract {
        override fun onBack() {}
    }
}