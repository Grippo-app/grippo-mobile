package com.grippo.analytics.home.internal.schema

import com.grippo.analytics.home.models.HomeOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object HomeOperationSchema {
    val Operation = AnalyticsParameterKey.code<HomeOperation>("home_operation") {
        when (it) {
            HomeOperation.RefreshTrainings -> "refresh_trainings"
            HomeOperation.RefreshExerciseExamples -> "refresh_exercise_examples"
            HomeOperation.SavePeriod -> "save_period"
        }
    }
    val Duration = AnalyticsParameterKey.duration("duration_ms")
    val Started = AnalyticsEventSchema("home_task_started", 1, listOf(Operation))
    val Completed = AnalyticsEventSchema("home_task_completed", 1, listOf(Operation, Duration))
    val Failed = AnalyticsEventSchema("home_task_failed", 1, listOf(Operation, Duration))
    val Interrupted = AnalyticsEventSchema("home_task_interrupted", 1, listOf(Operation, Duration))
    val all = listOf(Started, Completed, Failed, Interrupted)
}
