package com.grippo.analytics.training.internal.schema

import com.grippo.analytics.training.models.TrainingOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object TrainingOperationSchema {
    val Operation = AnalyticsParameterKey.code<TrainingOperation>("training_operation", encode = {
        when (it) {
            TrainingOperation.DeleteDraft -> "delete_draft"
            TrainingOperation.LoadDraft -> "load_draft"
            TrainingOperation.LoadTraining -> "load_training"
            TrainingOperation.SaveDraft -> "save_draft"
            TrainingOperation.CreateTraining -> "create_training"
            TrainingOperation.UpdateTraining -> "update_training"
            TrainingOperation.RefreshExamples -> "refresh_examples"
            TrainingOperation.RefreshTrainings -> "refresh_trainings"
            TrainingOperation.LoadRecords -> "load_records"
            TrainingOperation.ShareRecap -> "share_recap"
        }
    })
    val Duration = AnalyticsParameterKey.duration("duration_ms")

    val Started = AnalyticsEventSchema("training_task_started", 1, listOf(Operation))
    val Completed = AnalyticsEventSchema("training_task_completed", 1, listOf(Operation, Duration))
    val Failed = AnalyticsEventSchema("training_task_failed", 1, listOf(Operation, Duration))
    val Interrupted = AnalyticsEventSchema("training_task_interrupted", 1, listOf(Operation, Duration))
    val all = listOf(Started, Completed, Failed, Interrupted)
}
