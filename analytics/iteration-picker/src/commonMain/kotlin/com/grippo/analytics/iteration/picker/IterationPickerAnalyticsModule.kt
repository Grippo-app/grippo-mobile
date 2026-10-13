package com.grippo.analytics.iteration.picker

import com.grippo.analytics.iteration.picker.internal.schema.IterationPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class IterationPickerAnalyticsModule {
    @Single
    @Named("iteration_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(IterationPickerEventMatrix.all)
}
