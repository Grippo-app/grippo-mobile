package com.grippo.analytics.start.training.internal.schema

import com.grippo.analytics.start.training.models.StartTrainingOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object StartTrainingOperationSchema {
    val Operation = AnalyticsParameterKey.code<StartTrainingOperation>("start_training_operation") {
        when (it) {
            StartTrainingOperation.BuildOptions -> "build_options"
        }
    }
    val Duration = AnalyticsParameterKey.duration("duration_ms")
    val Started = AnalyticsEventSchema("start_training_task_started", 1, listOf(Operation))
    val Completed = AnalyticsEventSchema("start_training_task_completed", 1, listOf(Operation, Duration))
    val Failed = AnalyticsEventSchema("start_training_task_failed", 1, listOf(Operation, Duration))
    val Interrupted = AnalyticsEventSchema("start_training_task_interrupted", 1, listOf(Operation, Duration))
    val all = listOf(Started, Completed, Failed, Interrupted)
}
