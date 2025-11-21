package com.grippo.authorization.registration.excluded.muscles

import androidx.compose.runtime.Immutable

@Immutable
internal interface ExcludedMusclesContract {
    fun onSelect(id: String)
    fun onNextClick()
    fun onBack()

    @Immutable
    companion object Empty : ExcludedMusclesContract {
        override fun onSelect(id: String) {}
        override fun onNextClick() {}
        override fun onBack() {}
    }
}