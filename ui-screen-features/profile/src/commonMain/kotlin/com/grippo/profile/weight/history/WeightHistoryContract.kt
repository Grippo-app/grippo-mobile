package com.grippo.profile.weight.history

internal interface WeightHistoryContract {
    fun onWeightPickerClick()
    fun onBack()

    companion object Empty : WeightHistoryContract {
        override fun onWeightPickerClick() {}
        override fun onBack() {}
    }
}