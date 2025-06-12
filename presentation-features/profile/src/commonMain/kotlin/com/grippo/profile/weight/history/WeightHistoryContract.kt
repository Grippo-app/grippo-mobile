package com.grippo.profile.weight.history

internal interface WeightHistoryContract {
    fun back()

    companion object Empty : WeightHistoryContract {
        override fun back() {}
    }
}