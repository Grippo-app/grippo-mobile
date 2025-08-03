package com.grippo.error.display

internal interface ErrorDisplayContract {
    fun onDismiss()

    companion object Empty : ErrorDisplayContract {
        override fun onDismiss() {}
    }
}