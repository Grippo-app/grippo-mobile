package com.grippo.home

import androidx.compose.runtime.Immutable

@Immutable
internal interface HomeContract {
    fun onBack()
    fun toExcludedMuscles()
    fun toMissingEquipment()
    fun toWeightHistory()
    fun toDebug()
    fun toEditTraining(id: String)
    fun toAddTraining()

    @Immutable
    companion object Empty : HomeContract {
        override fun onBack() {}
        override fun toExcludedMuscles() {}
        override fun toMissingEquipment() {}
        override fun toWeightHistory() {}
        override fun toDebug() {}
        override fun toEditTraining(id: String) {}
        override fun toAddTraining() {}
    }
}