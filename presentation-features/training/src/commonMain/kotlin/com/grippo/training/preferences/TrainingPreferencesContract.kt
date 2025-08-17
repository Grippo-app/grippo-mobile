package com.grippo.training.preferences

internal interface TrainingPreferencesContract {
    fun onToggleMuscle(id: String)
    fun onContinueClick()
    fun onBack()

    companion object Empty : TrainingPreferencesContract {
        override fun onToggleMuscle(id: String) {}
        override fun onContinueClick() {}
        override fun onBack() {}
    }
}