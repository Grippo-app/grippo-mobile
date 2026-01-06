package com.grippo.home.home

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeContract {
    fun onOpenProfile()
    fun onStartTraining()
    fun onOpenTrainings()
    fun onOpenExample(id: String)
    fun onOpenMuscleLoading()
    fun onOpenWeeklyDigest()
    fun onOpenMonthlyDigest()
    fun onBack()

    @Immutable
    companion object Empty : HomeContract {
        override fun onOpenProfile() {}
        override fun onStartTraining() {}
        override fun onOpenTrainings() {}
        override fun onOpenMuscleLoading() {}
        override fun onOpenExample(id: String) {}
        override fun onOpenWeeklyDigest() {}
        override fun onOpenMonthlyDigest() {}
        override fun onBack() {}
    }
}
