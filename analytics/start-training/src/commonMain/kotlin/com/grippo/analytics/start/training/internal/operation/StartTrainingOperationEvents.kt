package com.grippo.analytics.start.training.internal.operation

import com.grippo.analytics.start.training.internal.events.StartTrainingPayloadEvent
import com.grippo.analytics.start.training.internal.schema.StartTrainingOperationSchema
import com.grippo.analytics.start.training.models.StartTrainingOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class StartTrainingOperationEvents<T>(private val operation: StartTrainingOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = StartTrainingPayloadEvent(
        StartTrainingOperationSchema.Started,
        listOf(StartTrainingOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(StartTrainingOperationSchema.Completed, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(StartTrainingOperationSchema.Failed, duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(StartTrainingOperationSchema.Interrupted, duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = StartTrainingPayloadEvent(
        schema,
        listOf(StartTrainingOperationSchema.Operation.value(operation), StartTrainingOperationSchema.Duration.value(duration)),
    )
}
