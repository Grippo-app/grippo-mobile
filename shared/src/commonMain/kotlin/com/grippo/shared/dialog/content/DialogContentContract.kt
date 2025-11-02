package com.grippo.shared.dialog.content

import androidx.compose.runtime.Immutable

@Immutable
internal interface DialogContentContract {
    fun onBack(pendingResult: (() -> Unit)?)

    @Immutable
    companion object Empty : DialogContentContract {
        override fun onBack(pendingResult: (() -> Unit)?) {}
    }
}