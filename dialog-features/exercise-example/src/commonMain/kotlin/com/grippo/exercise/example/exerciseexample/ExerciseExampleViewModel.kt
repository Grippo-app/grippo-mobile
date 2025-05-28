package com.grippo.exercise.example.exerciseexample

import com.grippo.core.BaseViewModel

internal class ExerciseExampleViewModel :
    BaseViewModel<ExerciseExampleState, ExerciseExampleDirection, ExerciseExampleLoader>(
        ExerciseExampleState
    ), ExerciseExampleContract