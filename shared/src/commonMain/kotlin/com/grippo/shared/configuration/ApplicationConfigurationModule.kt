package com.grippo.shared.configuration

import com.grippo.toolkit.analytics.core.AnalyticsConfiguration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
public expect class ApplicationConfigurationModule() {
    @Single
    internal fun provideAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration
}
