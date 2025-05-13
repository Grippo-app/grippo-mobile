package com.grippo.authorization.registration.completed

internal interface CompletedContract {
    fun complete()

    companion object Empty : CompletedContract {
        override fun complete() {}
    }
}