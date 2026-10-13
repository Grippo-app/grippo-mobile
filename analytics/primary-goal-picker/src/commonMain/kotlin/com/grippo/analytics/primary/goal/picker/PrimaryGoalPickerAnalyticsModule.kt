package com.grippo.analytics.primary.goal.picker

import com.grippo.analytics.primary.goal.picker.internal.schema.PrimaryGoalPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class PrimaryGoalPickerAnalyticsModule {
    @Single
    @Named("primary_goal_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(PrimaryGoalPickerEventMatrix.all)
}
