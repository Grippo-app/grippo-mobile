package com.grippo.analytics.training.goal.details

import com.grippo.analytics.training.goal.details.internal.schema.TrainingGoalDetailsEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class TrainingGoalDetailsAnalyticsModule {
    @Single
    @Named("training_goal_details_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(TrainingGoalDetailsEventMatrix.all)
}
