package com.grippo.iteration.picker

internal interface IterationPickerContract {
    fun onVolumeChange(value: String)
    fun onRepetitionsChange(value: String)
    fun onIterationClick(id: String)
    fun onSubmit()
    fun onBack()

    companion object Empty : IterationPickerContract {
        override fun onVolumeChange(value: String) {}
        override fun onRepetitionsChange(value: String) {}
        override fun onIterationClick(id: String) {}
        override fun onSubmit() {}
        override fun onBack() {}
    }
}