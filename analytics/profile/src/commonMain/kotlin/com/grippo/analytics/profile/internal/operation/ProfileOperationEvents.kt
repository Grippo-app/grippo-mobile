package com.grippo.analytics.profile.internal.operation

import com.grippo.analytics.profile.internal.events.ProfilePayloadEvent
import com.grippo.analytics.profile.internal.schema.ProfileOperationSchema
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class ProfileOperationEvents<T>(private val operation: ProfileOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = ProfilePayloadEvent(
        ProfileOperationSchema.Started,
        listOf(ProfileOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(ProfileOperationSchema.Completed, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(ProfileOperationSchema.Failed, duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(ProfileOperationSchema.Interrupted, duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = ProfilePayloadEvent(
        schema,
        listOf(ProfileOperationSchema.Operation.value(operation), ProfileOperationSchema.Duration.value(duration)),
    )
}
