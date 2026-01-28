package com.grippo.draft.training

import androidx.compose.runtime.Immutable

@Immutable
internal interface DraftTrainingContract {
    fun onContinue()
    fun onStartNew()
    fun onBack()

    @Immutable
    companion object Empty : DraftTrainingContract {
        override fun onContinue() {}
        override fun onStartNew() {}
        override fun onBack() {}
    }
}
