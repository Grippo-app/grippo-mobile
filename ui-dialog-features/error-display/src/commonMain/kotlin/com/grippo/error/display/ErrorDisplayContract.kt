package com.grippo.error.display

import androidx.compose.runtime.Immutable

@Immutable
internal interface ErrorDisplayContract {
    fun onAction()
    fun onDismiss()

    @Immutable
    companion object Empty : ErrorDisplayContract {
        override fun onAction() {}
        override fun onDismiss() {}
    }
}