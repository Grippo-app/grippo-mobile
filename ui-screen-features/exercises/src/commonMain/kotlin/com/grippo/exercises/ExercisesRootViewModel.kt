package com.grippo.exercises

import com.grippo.core.foundation.BaseViewModel

public class ExercisesRootViewModel :
    BaseViewModel<ExercisesRootState, ExercisesRootDirection, ExercisesRootLoader>(ExercisesRootState),
    ExercisesRootContract {
    override fun onBack() {
        navigateTo(ExercisesRootDirection.Back)
    }
}
