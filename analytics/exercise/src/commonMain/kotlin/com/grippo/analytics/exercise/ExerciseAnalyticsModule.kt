package com.grippo.analytics.exercise

import com.grippo.analytics.exercise.internal.schema.ExerciseEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class ExerciseAnalyticsModule {
    @Single
    @Named("exercise_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(ExerciseEventMatrix.all)
}
