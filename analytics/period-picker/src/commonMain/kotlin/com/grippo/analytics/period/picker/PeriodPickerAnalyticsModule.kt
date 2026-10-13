package com.grippo.analytics.period.picker

import com.grippo.analytics.period.picker.internal.schema.PeriodPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class PeriodPickerAnalyticsModule {
    @Single
    @Named("period_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(PeriodPickerEventMatrix.all)
}
