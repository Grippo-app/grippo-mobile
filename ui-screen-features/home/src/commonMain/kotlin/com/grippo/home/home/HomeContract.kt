package com.grippo.home.home

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeContract {
    fun onOpenProfile()
    fun onStartTraining()
    fun onOpenTrainings()
    fun onBack()

    @Immutable
    companion object Empty : HomeContract {
        override fun onOpenProfile() {}
        override fun onStartTraining() {}
        override fun onOpenTrainings() {}
        override fun onBack() {}
    }
}
