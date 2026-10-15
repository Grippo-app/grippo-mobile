package com.grippo.home

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeRootContract {
    fun onBack()
    fun toTrainings()
    fun toGoal()

    @Immutable
    companion object Empty : HomeRootContract {
        override fun onBack() {}
        override fun toTrainings() {}
        override fun toGoal() {}
    }
}
