package com.grippo.training.setup

internal interface TrainingSetupContract {
    fun onToggleMuscle(id: String)
    fun onContinueClick()
    fun onBack()

    companion object Empty : TrainingSetupContract {
        override fun onToggleMuscle(id: String) {}
        override fun onContinueClick() {}
        override fun onBack() {}
    }
}