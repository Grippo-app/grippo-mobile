package com.grippo.shared.dialog

internal interface DialogContract {

    fun dismiss()

    companion object Empty : DialogContract {
        override fun dismiss() {}
    }
}