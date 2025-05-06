package com.grippo.shared.dialog

internal interface DialogContract {

    fun dismiss()
    fun release()

    companion object Empty : DialogContract {
        override fun dismiss() {}
        override fun release() {}
    }
}