package com.grippo.home

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeRootContract {
    fun onBack()
    fun toExcludedMuscles()
    fun toMissingEquipment()
    fun toExperience()
    fun toDebug()
    fun toTrainings()
    fun toAddTraining()
    fun toDraftTraining()
    fun toSettings()
    fun toWeightAndHeight()

    @Immutable
    companion object Empty : HomeRootContract {
        override fun onBack() {}
        override fun toWeightAndHeight() {}
        override fun toExcludedMuscles() {}
        override fun toMissingEquipment() {}
        override fun toExperience() {}
        override fun toDebug() {}
        override fun toTrainings() {}
        override fun toAddTraining() {}
        override fun toDraftTraining() {}
        override fun toSettings() {}
    }
}
