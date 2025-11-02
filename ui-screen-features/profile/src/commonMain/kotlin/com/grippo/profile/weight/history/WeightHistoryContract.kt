package com.grippo.profile.weight.history

import androidx.compose.runtime.Immutable

@Immutable
internal interface WeightHistoryContract {
    fun onWeightPickerClick()
    fun onBack()

    @Immutable
    companion object Empty : WeightHistoryContract {
        override fun onWeightPickerClick() {}
        override fun onBack() {}
    }
}