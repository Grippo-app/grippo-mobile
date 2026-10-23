package com.grippo.shared.root

import androidx.compose.runtime.Immutable
import com.grippo.core.state.stage.StageState
import com.grippo.screen.api.ExercisesRouter

@Immutable
public interface RootContract {
    public fun onClose()
    public fun toHome()
    public fun toDebug()
    public fun toTrainings()
    public fun toTraining(stage: StageState)
    public fun toExercises(route: ExercisesRouter.Library)
    public fun toGoal()
    public fun onBack()

    @Immutable
    public companion object Empty : RootContract {
        override fun onClose() {}
        override fun toHome() {}
        override fun toDebug() {}
        override fun toTrainings() {}
        override fun toTraining(stage: StageState) {}
        override fun toExercises(route: ExercisesRouter.Library) {}
        override fun toGoal() {}
        override fun onBack() {}
    }
}
