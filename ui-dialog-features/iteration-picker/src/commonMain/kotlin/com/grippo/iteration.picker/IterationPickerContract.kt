package com.grippo.iteration.picker

import androidx.compose.runtime.Immutable

@Immutable
internal interface IterationPickerContract {
    fun onExternalWeightChange(value: String)
    fun onExtraWeightChange(value: String)
    fun onAssistWeightChange(value: String)
    fun onRepetitionsChange(value: String)
    fun onIterationClick(id: String)
    fun onSubmit()
    fun onBack()

    @Immutable
    companion object Empty : IterationPickerContract {
        override fun onExternalWeightChange(value: String) {}
        override fun onExtraWeightChange(value: String) {}
        override fun onAssistWeightChange(value: String) {}
        override fun onRepetitionsChange(value: String) {}
        override fun onIterationClick(id: String) {}
        override fun onSubmit() {}
        override fun onBack() {}
    }
}