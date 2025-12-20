package com.grippo.home

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeRootContract {
    fun onBack()
    fun toExcludedMuscles()
    fun toMissingEquipment()
    fun toWeightHistory()
    fun toDebug()
    fun toTrainings()
    fun toAddTraining()

    @Immutable
    companion object Empty : HomeRootContract {
        override fun onBack() {}
        override fun toExcludedMuscles() {}
        override fun toMissingEquipment() {}
        override fun toWeightHistory() {}
        override fun toDebug() {}
        override fun toTrainings() {}
        override fun toAddTraining() {}
    }
}
