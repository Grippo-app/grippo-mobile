package com.grippo.analytics.exercise.internal.operation

import com.grippo.analytics.exercise.internal.events.ExercisePayloadEvent
import com.grippo.analytics.exercise.internal.schema.ExerciseOperationSchema
import com.grippo.analytics.exercise.models.ExerciseOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class ExerciseOperationEvents<T>(private val operation: ExerciseOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = ExercisePayloadEvent(
        schema = ExerciseOperationSchema.Started,
        parameters = listOf(ExerciseOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(schema = ExerciseOperationSchema.Completed, duration = duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(schema = ExerciseOperationSchema.Failed, duration = duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(schema = ExerciseOperationSchema.Interrupted, duration = duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = ExercisePayloadEvent(
        schema = schema,
        parameters = listOf(
            ExerciseOperationSchema.Operation.value(operation),
            ExerciseOperationSchema.Duration.value(duration),
        ),
    )
}
