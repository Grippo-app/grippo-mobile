package com.grippo.analytics.error.display

import com.grippo.analytics.error.display.internal.schema.ErrorDisplayEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class ErrorDisplayAnalyticsModule {
    @Single
    @Named("error_display_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(ErrorDisplayEventMatrix.all)
}
