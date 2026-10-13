package com.grippo.analytics.home.internal.operation

import com.grippo.analytics.home.internal.events.HomePayloadEvent
import com.grippo.analytics.home.internal.schema.HomeOperationSchema
import com.grippo.analytics.home.models.HomeOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class HomeOperationEvents<T>(private val operation: HomeOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = HomePayloadEvent(
        HomeOperationSchema.Started,
        listOf(HomeOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(HomeOperationSchema.Completed, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(HomeOperationSchema.Failed, duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(HomeOperationSchema.Interrupted, duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = HomePayloadEvent(
        schema,
        listOf(HomeOperationSchema.Operation.value(operation), HomeOperationSchema.Duration.value(duration)),
    )
}
