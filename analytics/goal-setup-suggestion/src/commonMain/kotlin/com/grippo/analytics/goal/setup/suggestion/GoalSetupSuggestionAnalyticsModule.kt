package com.grippo.analytics.goal.setup.suggestion

import com.grippo.analytics.goal.setup.suggestion.internal.schema.GoalSetupSuggestionEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class GoalSetupSuggestionAnalyticsModule {
    @Single
    @Named("goal_setup_suggestion_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(GoalSetupSuggestionEventMatrix.all)
}
