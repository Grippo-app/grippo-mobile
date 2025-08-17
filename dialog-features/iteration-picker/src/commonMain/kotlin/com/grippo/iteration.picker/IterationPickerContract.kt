package com.grippo.iteration.picker

internal interface IterationPickerContract {
    fun onWeightChange(value: Float)
    fun onRepetitionsChange(value: Int)
    fun onSubmit()
    fun onBack()

    companion object Empty : IterationPickerContract {
        override fun onWeightChange(value: Float) {}
        override fun onRepetitionsChange(value: Int) {}
        override fun onSubmit() {}
        override fun onBack() {}
    }
}