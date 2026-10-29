package com.grippo.home

import androidx.compose.runtime.Immutable
import com.grippo.screen.api.TrainingsRouter

@Immutable
internal interface HomeRootContract {
    fun onBack()
    fun toTrainings(route: TrainingsRouter)
    fun toGoal()

    @Immutable
    companion object Empty : HomeRootContract {
        override fun onBack() {}
        override fun toTrainings(route: TrainingsRouter) {}
        override fun toGoal() {}
    }
}
