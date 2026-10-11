package com.grippo.toolkit.analytics.core

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.core.internal.AnalyticsCatalog
import com.grippo.toolkit.analytics.core.internal.AnalyticsConfiguration
import com.grippo.toolkit.analytics.core.internal.AnalyticsValidationMode
import com.grippo.toolkit.analytics.core.internal.DefaultAnalytics
import com.grippo.toolkit.analytics.core.internal.createAnalyticsConfiguration
import com.grippo.toolkit.analytics.lifecycle.AnalyticsBootstrap
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
public class AnalyticsModule {
    @Single
    internal fun provideAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration = createAnalyticsConfiguration(scope)

    @Single
    internal fun provideAnalyticsCatalog(scope: Scope): AnalyticsCatalog =
        AnalyticsCatalog.fromSchemas(scope.getAll<AnalyticsSchema>())

    @Single(binds = [Analytics::class, AnalyticsBootstrap::class])
    internal fun provideAnalytics(catalog: AnalyticsCatalog, configuration: AnalyticsConfiguration): DefaultAnalytics =
        DefaultAnalytics(
            catalog = catalog,
            diagnosticCapacity = if (configuration.debug) 64 else 0,
            validationMode = if (configuration.debug) AnalyticsValidationMode.Throw else AnalyticsValidationMode.Reject,
        )
}
