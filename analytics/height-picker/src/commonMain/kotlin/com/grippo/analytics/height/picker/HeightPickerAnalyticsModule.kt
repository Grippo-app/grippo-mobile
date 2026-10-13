package com.grippo.analytics.height.picker

import com.grippo.analytics.height.picker.internal.schema.HeightPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class HeightPickerAnalyticsModule {
    @Single
    @Named("height_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(HeightPickerEventMatrix.all)
}
