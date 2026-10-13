package com.grippo.analytics.month.picker

import com.grippo.analytics.month.picker.internal.schema.MonthPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class MonthPickerAnalyticsModule {
    @Single
    @Named("month_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(MonthPickerEventMatrix.all)
}
