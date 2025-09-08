package com.grippo.drart.training

internal interface DraftTrainingContract {
    fun onContinue()
    fun onDelete()
    fun onBack()

    companion object Empty : DraftTrainingContract {
        override fun onContinue() {}
        override fun onDelete() {}
        override fun onBack() {}
    }
}
