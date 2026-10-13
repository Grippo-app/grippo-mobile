package com.grippo.analytics.statistics

import com.grippo.analytics.statistics.internal.schema.StatisticsEventMatrix
import com.grippo.analytics.statistics.internal.schema.StatisticsOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class StatisticsAnalyticsModule {
    @Single
    @Named("statistics_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(StatisticsEventMatrix.all + StatisticsOperationSchema.all)
}
