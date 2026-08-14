package com.grippo.main

import androidx.compose.runtime.Immutable

@Immutable
internal interface MainContract {
    fun onTabSelected(index: Int)
    fun onStartTraining()

    @Immutable
    companion object Empty : MainContract {
        override fun onTabSelected(index: Int) {}
        override fun onStartTraining() {}
    }
}
