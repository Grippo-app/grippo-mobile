package com.grippo.analytics.exercise.example

import com.grippo.analytics.exercise.example.internal.schema.ExerciseExampleEventMatrix
import com.grippo.analytics.exercise.example.internal.schema.ExerciseExampleOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class ExerciseExampleAnalyticsModule {
    @Single
    @Named("exercise_example_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(ExerciseExampleEventMatrix.all + ExerciseExampleOperationSchema.all)
}
