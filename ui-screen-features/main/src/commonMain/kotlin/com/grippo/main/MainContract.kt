package com.grippo.main

import androidx.compose.runtime.Immutable
import com.grippo.screen.api.MainRouter
import com.grippo.screen.api.TrainingsRouter

@Immutable
internal interface MainContract {
    fun onSelectTab(tab: MainRouter)
    fun onStartTraining()
    fun toTrainings(route: TrainingsRouter)
    fun onBack()

    @Immutable
    companion object Empty : MainContract {
        override fun onSelectTab(tab: MainRouter) {}
        override fun onStartTraining() {}
        override fun toTrainings(route: TrainingsRouter) {}
        override fun onBack() {}
    }
}
