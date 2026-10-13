package com.grippo.exercise.example.exerciseexample.analytics

import com.grippo.analytics.exercise.example.models.ExerciseExampleTab
import com.grippo.exercise.example.exerciseexample.ExerciseExampleTabState

internal fun ExerciseExampleTabState.toAnalytics(): ExerciseExampleTab = when (this) {
    ExerciseExampleTabState.Information -> ExerciseExampleTab.Information
    ExerciseExampleTabState.Achievements -> ExerciseExampleTab.Achievements
}
