package com.grippo.analytics.weight.picker

import com.grippo.analytics.weight.picker.internal.schema.WeightPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class WeightPickerAnalyticsModule {
    @Single
    @Named("weight_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(WeightPickerEventMatrix.all)
}
