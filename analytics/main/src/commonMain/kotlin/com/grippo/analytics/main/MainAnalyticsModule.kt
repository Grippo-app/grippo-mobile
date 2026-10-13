package com.grippo.analytics.main

import com.grippo.analytics.main.internal.schema.MainEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class MainAnalyticsModule {
    @Single
    @Named("main_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(MainEventMatrix.all)
}
