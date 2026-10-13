package com.grippo.analytics.exercises

import com.grippo.analytics.exercises.internal.schema.ExercisesEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class ExercisesAnalyticsModule {
    @Single
    @Named("exercises_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(ExercisesEventMatrix.all)
}
