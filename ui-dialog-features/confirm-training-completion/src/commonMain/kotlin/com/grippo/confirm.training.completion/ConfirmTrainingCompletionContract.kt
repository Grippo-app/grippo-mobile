package com.grippo.confirm.training.completion

import androidx.compose.runtime.Immutable

@Immutable
internal interface ConfirmTrainingCompletionContract {
    fun onDurationInputClick()
    fun onConfirm()
    fun onBack()

    @Immutable
    companion object Empty : ConfirmTrainingCompletionContract {
        override fun onDurationInputClick() {}
        override fun onConfirm() {}
        override fun onBack() {}
    }
}
