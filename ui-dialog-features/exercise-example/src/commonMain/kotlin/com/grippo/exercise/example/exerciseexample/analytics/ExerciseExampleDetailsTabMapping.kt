package com.grippo.exercise.example.exerciseexample.analytics

import com.grippo.analytics.exercise.example.models.ExerciseExampleTab
import com.grippo.exercise.example.exerciseexample.ExerciseExampleDetailsTabState

internal fun ExerciseExampleDetailsTabState.toAnalytics(): ExerciseExampleTab = when (this) {
    ExerciseExampleDetailsTabState.Information -> ExerciseExampleTab.Information
    ExerciseExampleDetailsTabState.Achievements -> ExerciseExampleTab.Achievements
}
