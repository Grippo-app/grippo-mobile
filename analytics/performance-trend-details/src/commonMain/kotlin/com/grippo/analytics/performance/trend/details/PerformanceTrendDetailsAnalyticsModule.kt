package com.grippo.analytics.performance.trend.details

import com.grippo.analytics.performance.trend.details.internal.schema.PerformanceTrendDetailsEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class PerformanceTrendDetailsAnalyticsModule {
    @Single
    @Named("performance_trend_details_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(PerformanceTrendDetailsEventMatrix.all)
}
