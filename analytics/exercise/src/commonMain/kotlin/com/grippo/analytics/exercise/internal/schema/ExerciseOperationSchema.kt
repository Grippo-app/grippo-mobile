package com.grippo.analytics.exercise.internal.schema

import com.grippo.analytics.exercise.models.ExerciseOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object ExerciseOperationSchema {
    val Operation = AnalyticsParameterKey.code<ExerciseOperation>("exercise_operation") {
        when (it) {
            ExerciseOperation.LoadHistory -> "load_history"
        }
    }
    val Duration = AnalyticsParameterKey.duration("duration_ms")
    val Started = AnalyticsEventSchema(name = "exercise_task_started", version = 1, parameters = listOf(Operation))
    val Completed = AnalyticsEventSchema(
        name = "exercise_task_completed",
        version = 1,
        parameters = listOf(Operation, Duration),
    )
    val Failed = AnalyticsEventSchema(
        name = "exercise_task_failed",
        version = 1,
        parameters = listOf(Operation, Duration),
    )
    val Interrupted = AnalyticsEventSchema(
        name = "exercise_task_interrupted",
        version = 1,
        parameters = listOf(Operation, Duration),
    )
    val all = listOf(Started, Completed, Failed, Interrupted)
}
