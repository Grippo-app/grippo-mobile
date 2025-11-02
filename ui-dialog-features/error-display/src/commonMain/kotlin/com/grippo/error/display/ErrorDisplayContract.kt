package com.grippo.error.display

import androidx.compose.runtime.Immutable

@Immutable
internal interface ErrorDisplayContract {
    fun onDismiss()

    @Immutable
    companion object Empty : ErrorDisplayContract {
        override fun onDismiss() {}
    }
}