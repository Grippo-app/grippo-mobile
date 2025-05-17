package com.grippo.error.display

internal interface ErrorDisplayContract {
    fun next()

    companion object Empty : ErrorDisplayContract {
        override fun next() {}
    }
}