package com.grippo.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.stage.TrainingSeed

@Immutable
internal interface TrainingsRootContract {
    fun onBack()
    fun toEditTraining(id: String)
    fun toStartTraining(seed: TrainingSeed)

    @Immutable
    companion object Empty : TrainingsRootContract {
        override fun onBack() {}
        override fun toEditTraining(id: String) {}
        override fun toStartTraining(seed: TrainingSeed) {}
    }
}
