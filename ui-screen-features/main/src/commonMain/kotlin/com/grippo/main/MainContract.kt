package com.grippo.main

import androidx.compose.runtime.Immutable
import com.grippo.screen.api.MainRouter

@Immutable
internal interface MainContract {
    fun onSelectTab(tab: MainRouter)
    fun onStartTraining()
    fun onBack()

    @Immutable
    companion object Empty : MainContract {
        override fun onSelectTab(tab: MainRouter) {}
        override fun onStartTraining() {}
        override fun onBack() {}
    }
}
