package com.grippo.exercise.examples

import com.grippo.core.BaseViewModel

public class ExerciseExamplesViewModel :
    BaseViewModel<ExerciseExamplesState, ExerciseExamplesDirection, ExerciseExamplesLoader>(
        ExerciseExamplesState
    ), ExerciseExamplesContract {

    override fun onBack() {
        navigateTo(ExerciseExamplesDirection.Back)
    }
}
