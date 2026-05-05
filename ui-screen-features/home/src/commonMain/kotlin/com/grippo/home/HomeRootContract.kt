package com.grippo.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.stage.TrainingSeed

@Immutable
internal interface HomeRootContract {
    fun onBack()
    fun toExcludedMuscles()
    fun toMissingEquipment()
    fun toExperience()
    fun toDebug()
    fun toTrainings()
    fun toStartTraining(seed: TrainingSeed)
    fun toDraftTraining()
    fun toSettings()
    fun toSocial()
    fun toBody()
    fun toGoal()

    @Immutable
    companion object Empty : HomeRootContract {
        override fun onBack() {}
        override fun toBody() {}
        override fun toExcludedMuscles() {}
        override fun toMissingEquipment() {}
        override fun toExperience() {}
        override fun toDebug() {}
        override fun toTrainings() {}
        override fun toStartTraining(seed: TrainingSeed) {}
        override fun toDraftTraining() {}
        override fun toSettings() {}
        override fun toSocial() {}
        override fun toGoal() {}
    }
}
