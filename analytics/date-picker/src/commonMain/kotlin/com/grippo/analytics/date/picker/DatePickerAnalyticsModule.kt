package com.grippo.analytics.date.picker

import com.grippo.analytics.date.picker.internal.schema.DatePickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class DatePickerAnalyticsModule {
    @Single
    @Named("date_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(DatePickerEventMatrix.all)
}
