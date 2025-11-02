package com.grippo.confirmation

import androidx.compose.runtime.Immutable

@Immutable
internal interface ConfirmationContract {
    fun onConfirm()
    fun onBack()

    @Immutable
    companion object Empty : ConfirmationContract {
        override fun onConfirm() {}
        override fun onBack() {}
    }
}
