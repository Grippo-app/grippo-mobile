package com.grippo.analytics.statistics.internal.operation

import com.grippo.analytics.statistics.internal.events.StatisticsPayloadEvent
import com.grippo.analytics.statistics.internal.schema.StatisticsOperationSchema
import com.grippo.analytics.statistics.models.StatisticsOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class StatisticsOperationEvents<T>(private val operation: StatisticsOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = StatisticsPayloadEvent(
        StatisticsOperationSchema.Started,
        listOf(StatisticsOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(StatisticsOperationSchema.Completed, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(StatisticsOperationSchema.Failed, duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(StatisticsOperationSchema.Interrupted, duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = StatisticsPayloadEvent(
        schema,
        listOf(StatisticsOperationSchema.Operation.value(operation), StatisticsOperationSchema.Duration.value(duration)),
    )
}
