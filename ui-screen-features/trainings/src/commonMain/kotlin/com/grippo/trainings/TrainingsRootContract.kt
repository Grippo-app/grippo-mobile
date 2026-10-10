package com.grippo.trainings

import androidx.compose.runtime.Immutable

@Immutable
internal interface TrainingsRootContract {
    fun onBack()
    fun toEditTraining(id: String)

    @Immutable
    companion object Empty : TrainingsRootContract {
        override fun onBack() {}
        override fun toEditTraining(id: String) {}
    }
}
