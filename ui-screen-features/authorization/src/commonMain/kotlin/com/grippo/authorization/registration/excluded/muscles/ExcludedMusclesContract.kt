package com.grippo.authorization.registration.excluded.muscles

internal interface ExcludedMusclesContract {
    fun onSelect(id: String)
    fun onNextClick()
    fun onBack()

    companion object Empty : ExcludedMusclesContract {
        override fun onSelect(id: String) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}