package com.grippo.analytics.exercise.example.internal.schema

import com.grippo.analytics.exercise.example.models.ExerciseExampleOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object ExerciseExampleOperationSchema {
    val Operation = AnalyticsParameterKey.code<ExerciseExampleOperation>("exercise_example_operation") {
        when (it) {
            ExerciseExampleOperation.LoadExample -> "load_example"
            ExerciseExampleOperation.LoadRecentExercises -> "load_recent_exercises"
            ExerciseExampleOperation.LoadAchievements -> "load_achievements"
        }
    }
    val Duration = AnalyticsParameterKey.duration("duration_ms")
    val Started = AnalyticsEventSchema("exercise_example_task_started", 1, listOf(Operation))
    val Completed = AnalyticsEventSchema("exercise_example_task_completed", 1, listOf(Operation, Duration))
    val Failed = AnalyticsEventSchema("exercise_example_task_failed", 1, listOf(Operation, Duration))
    val Interrupted = AnalyticsEventSchema("exercise_example_task_interrupted", 1, listOf(Operation, Duration))
    val all = listOf(Started, Completed, Failed, Interrupted)
}
