package com.grippo.authorization.registration.completed

internal interface CompletedContract {
    fun complete()
    fun back()

    companion object Empty : CompletedContract {
        override fun complete() {}
        override fun back() {}
    }
}