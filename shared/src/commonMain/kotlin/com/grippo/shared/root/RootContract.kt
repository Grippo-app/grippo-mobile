package com.grippo.shared.root

import androidx.compose.runtime.Immutable
import com.grippo.core.state.stage.StageState

@Immutable
public interface RootContract {
    public fun onClose()
    public fun toHome()
    public fun toProfile()
    public fun toDebug()
    public fun toTrainings()
    public fun toTraining(stage: StageState)
    public fun toWeightHistory()
    public fun toMissingEquipment()
    public fun toExcludedMuscles()
    public fun onBack()

    @Immutable
    public companion object Empty : RootContract {
        override fun onClose() {}
        override fun toHome() {}
        override fun toProfile() {}
        override fun toDebug() {}
        override fun toTrainings() {}
        override fun toTraining(stage: StageState) {}
        override fun toWeightHistory() {}
        override fun toMissingEquipment() {}
        override fun toExcludedMuscles() {}
        override fun onBack() {}
    }
}