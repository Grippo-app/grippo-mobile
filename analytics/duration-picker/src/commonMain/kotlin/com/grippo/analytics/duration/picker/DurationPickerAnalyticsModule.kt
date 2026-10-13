package com.grippo.analytics.duration.picker

import com.grippo.analytics.duration.picker.internal.schema.DurationPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class DurationPickerAnalyticsModule {
    @Single
    @Named("duration_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(DurationPickerEventMatrix.all)
}
