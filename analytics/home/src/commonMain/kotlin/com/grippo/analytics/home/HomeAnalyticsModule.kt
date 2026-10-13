package com.grippo.analytics.home

import com.grippo.analytics.home.internal.schema.HomeEventMatrix
import com.grippo.analytics.home.internal.schema.HomeOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class HomeAnalyticsModule {
    @Single
    @Named("home_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(HomeEventMatrix.all + HomeOperationSchema.all)
}
