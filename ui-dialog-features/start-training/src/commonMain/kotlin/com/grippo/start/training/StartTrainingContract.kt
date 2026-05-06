package com.grippo.start.training

import androidx.compose.runtime.Immutable

@Immutable
internal interface StartTrainingContract {
    fun onSelect(key: String)
    fun onBack()

    @Immutable
    companion object Empty : StartTrainingContract {
        override fun onSelect(key: String) {}
        override fun onBack() {}
    }
}
