package com.grippo.analytics.training.streak.details

import com.grippo.analytics.training.streak.details.internal.schema.TrainingStreakDetailsEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class TrainingStreakDetailsAnalyticsModule {
    @Single
    @Named("training_streak_details_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(TrainingStreakDetailsEventMatrix.all)
}
