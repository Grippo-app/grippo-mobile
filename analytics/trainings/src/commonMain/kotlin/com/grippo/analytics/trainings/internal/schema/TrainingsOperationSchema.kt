package com.grippo.analytics.trainings.internal.schema

import com.grippo.analytics.trainings.models.TrainingsOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object TrainingsOperationSchema {
    val Operation = AnalyticsParameterKey.code<TrainingsOperation>("trainings_operation") {
        when (it) {
            TrainingsOperation.RefreshTrainings -> "refresh_trainings"
            TrainingsOperation.DeleteTraining -> "delete_training"
        }
    }
    val Duration = AnalyticsParameterKey.duration("duration_ms")
    val Started = AnalyticsEventSchema("trainings_task_started", 1, listOf(Operation))
    val Completed = AnalyticsEventSchema("trainings_task_completed", 1, listOf(Operation, Duration))
    val Failed = AnalyticsEventSchema("trainings_task_failed", 1, listOf(Operation, Duration))
    val Interrupted = AnalyticsEventSchema("trainings_task_interrupted", 1, listOf(Operation, Duration))
    val all = listOf(Started, Completed, Failed, Interrupted)
}
