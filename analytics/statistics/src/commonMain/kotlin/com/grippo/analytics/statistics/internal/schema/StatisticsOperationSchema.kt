package com.grippo.analytics.statistics.internal.schema

import com.grippo.analytics.statistics.models.StatisticsOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object StatisticsOperationSchema {
    val Operation = AnalyticsParameterKey.code<StatisticsOperation>("statistics_operation") {
        when (it) {
            StatisticsOperation.RefreshTrainings -> "refresh_trainings"
        }
    }
    val Duration = AnalyticsParameterKey.duration("duration_ms")
    val Started = AnalyticsEventSchema("statistics_task_started", 1, listOf(Operation))
    val Completed = AnalyticsEventSchema("statistics_task_completed", 1, listOf(Operation, Duration))
    val Failed = AnalyticsEventSchema("statistics_task_failed", 1, listOf(Operation, Duration))
    val Interrupted = AnalyticsEventSchema("statistics_task_interrupted", 1, listOf(Operation, Duration))
    val all = listOf(Started, Completed, Failed, Interrupted)
}
