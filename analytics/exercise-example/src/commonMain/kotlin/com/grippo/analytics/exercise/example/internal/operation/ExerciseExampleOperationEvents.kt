package com.grippo.analytics.exercise.example.internal.operation

import com.grippo.analytics.exercise.example.internal.events.ExerciseExamplePayloadEvent
import com.grippo.analytics.exercise.example.internal.schema.ExerciseExampleOperationSchema
import com.grippo.analytics.exercise.example.models.ExerciseExampleOperation
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.values.DurationMillis

internal class ExerciseExampleOperationEvents<T>(private val operation: ExerciseExampleOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent = ExerciseExamplePayloadEvent(
        ExerciseExampleOperationSchema.Started,
        listOf(ExerciseExampleOperationSchema.Operation.value(operation)),
    )

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent =
        terminal(ExerciseExampleOperationSchema.Completed, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent =
        terminal(ExerciseExampleOperationSchema.Failed, duration)

    override fun interrupted(duration: DurationMillis): AnalyticsEvent =
        terminal(ExerciseExampleOperationSchema.Interrupted, duration)

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent = ExerciseExamplePayloadEvent(
        schema,
        listOf(ExerciseExampleOperationSchema.Operation.value(operation), ExerciseExampleOperationSchema.Duration.value(duration)),
    )
}
