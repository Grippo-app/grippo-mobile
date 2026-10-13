package com.grippo.analytics.trainings.internal.operation

import com.grippo.analytics.trainings.internal.events.TrainingsPayloadEvent
import com.grippo.analytics.trainings.internal.schema.TrainingsOperationSchema
import com.grippo.analytics.trainings.models.TrainingsOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class TrainingsOperationEvents<T>(private val operation: TrainingsOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = TrainingsPayloadEvent(
        TrainingsOperationSchema.Started,
        listOf(TrainingsOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(TrainingsOperationSchema.Completed, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(TrainingsOperationSchema.Failed, duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(TrainingsOperationSchema.Interrupted, duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = TrainingsPayloadEvent(
        schema,
        listOf(TrainingsOperationSchema.Operation.value(operation), TrainingsOperationSchema.Duration.value(duration)),
    )
}
