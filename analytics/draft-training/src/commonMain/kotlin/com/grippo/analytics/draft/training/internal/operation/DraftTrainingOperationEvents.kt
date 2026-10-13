package com.grippo.analytics.draft.training.internal.operation

import com.grippo.analytics.draft.training.internal.events.DraftTrainingPayloadEvent
import com.grippo.analytics.draft.training.internal.schema.DraftTrainingOperationSchema
import com.grippo.analytics.draft.training.models.DraftTrainingOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class DraftTrainingOperationEvents<T>(private val operation: DraftTrainingOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = DraftTrainingPayloadEvent(
        DraftTrainingOperationSchema.Started,
        listOf(DraftTrainingOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(DraftTrainingOperationSchema.Completed, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(DraftTrainingOperationSchema.Failed, duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(DraftTrainingOperationSchema.Interrupted, duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = DraftTrainingPayloadEvent(
        schema,
        listOf(DraftTrainingOperationSchema.Operation.value(operation), DraftTrainingOperationSchema.Duration.value(duration)),
    )
}
