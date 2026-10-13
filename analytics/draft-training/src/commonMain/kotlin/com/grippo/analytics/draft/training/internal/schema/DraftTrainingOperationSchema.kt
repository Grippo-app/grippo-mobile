package com.grippo.analytics.draft.training.internal.schema

import com.grippo.analytics.draft.training.models.DraftTrainingOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object DraftTrainingOperationSchema {
    val Operation = AnalyticsParameterKey.code<DraftTrainingOperation>("draft_training_operation") {
        when (it) {
            DraftTrainingOperation.DeleteDraft -> "delete_draft"
        }
    }
    val Duration = AnalyticsParameterKey.duration("duration_ms")
    val Started = AnalyticsEventSchema("draft_training_task_started", 1, listOf(Operation))
    val Completed = AnalyticsEventSchema("draft_training_task_completed", 1, listOf(Operation, Duration))
    val Failed = AnalyticsEventSchema("draft_training_task_failed", 1, listOf(Operation, Duration))
    val Interrupted = AnalyticsEventSchema("draft_training_task_interrupted", 1, listOf(Operation, Duration))
    val all = listOf(Started, Completed, Failed, Interrupted)
}
