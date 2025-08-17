package com.grippo.iteration.picker

internal interface IterationPickerContract {
    fun onWeightChange(value: String)
    fun onRepetitionsChange(value: String)
    fun onSubmit()
    fun onBack()

    companion object Empty : IterationPickerContract {
        override fun onWeightChange(value: String) {}
        override fun onRepetitionsChange(value: String) {}
        override fun onSubmit() {}
        override fun onBack() {}
    }
}