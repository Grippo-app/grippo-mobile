package com.grippo.training.setup

internal interface TrainingSetupContract {
    fun onSelect(id: String)
    fun onContinueClick()
    fun onBack()

    companion object Empty : TrainingSetupContract {
        override fun onSelect(id: String) {}
        override fun onContinueClick() {}
        override fun onBack() {}
    }
}