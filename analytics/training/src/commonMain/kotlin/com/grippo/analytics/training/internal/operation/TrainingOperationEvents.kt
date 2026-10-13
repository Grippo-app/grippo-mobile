package com.grippo.analytics.training.internal.operation

import com.grippo.analytics.training.internal.events.TrainingPayloadEvent
import com.grippo.analytics.training.internal.schema.TrainingOperationSchema
import com.grippo.analytics.training.models.TrainingOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class TrainingOperationEvents<T>(private val operation: TrainingOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = TrainingPayloadEvent(
        TrainingOperationSchema.Started,
        listOf(TrainingOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(TrainingOperationSchema.Completed, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(TrainingOperationSchema.Failed, duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(TrainingOperationSchema.Interrupted, duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = TrainingPayloadEvent(
        schema,
        listOf(TrainingOperationSchema.Operation.value(operation), TrainingOperationSchema.Duration.value(duration)),
    )
}
