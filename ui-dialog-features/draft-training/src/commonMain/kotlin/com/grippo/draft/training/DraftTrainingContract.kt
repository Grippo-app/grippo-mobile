package com.grippo.draft.training

import androidx.compose.runtime.Immutable

@Immutable
internal interface DraftTrainingContract {
    fun onContinue()
    fun onDelete()
    fun onBack()

    @Immutable
    companion object Empty : DraftTrainingContract {
        override fun onContinue() {}
        override fun onDelete() {}
        override fun onBack() {}
    }
}
