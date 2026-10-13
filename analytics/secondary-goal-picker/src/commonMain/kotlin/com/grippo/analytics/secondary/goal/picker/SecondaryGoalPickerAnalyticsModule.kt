package com.grippo.analytics.secondary.goal.picker

import com.grippo.analytics.secondary.goal.picker.internal.schema.SecondaryGoalPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class SecondaryGoalPickerAnalyticsModule {
    @Single
    @Named("secondary_goal_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(SecondaryGoalPickerEventMatrix.all)
}
