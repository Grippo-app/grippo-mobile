package com.grippo.error.display

internal interface ErrorDisplayContract {
    fun dismiss()

    companion object Empty : ErrorDisplayContract {
        override fun dismiss() {}
    }
}