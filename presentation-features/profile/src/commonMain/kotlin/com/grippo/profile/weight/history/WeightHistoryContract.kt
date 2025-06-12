package com.grippo.profile.weight.history

internal interface WeightHistoryContract {
    fun openWeightPicker()
    fun back()

    companion object Empty : WeightHistoryContract {
        override fun openWeightPicker() {}
        override fun back() {}
    }
}