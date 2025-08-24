package com.grippo.authorization.registration.excluded.muscles

internal interface ExcludedMusclesContract {
    fun onSelectMuscle(id: String)
    fun onNextClick()
    fun onBack()

    companion object Empty : ExcludedMusclesContract {
        override fun onSelectMuscle(id: String) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}