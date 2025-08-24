package com.grippo.confirmation

internal interface ConfirmationContract {
    fun onConfirm()
    fun onBack()

    companion object Empty : ConfirmationContract {
        override fun onConfirm() {}
        override fun onBack() {}
    }
}
