package com.grippo.shared.configuration

import com.grippo.toolkit.analytics.core.AnalyticsConfiguration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope
import kotlin.experimental.ExperimentalNativeApi

@Module
public actual class ApplicationConfigurationModule actual constructor() {
    @OptIn(ExperimentalNativeApi::class)
    @Single
    internal actual fun provideAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration =
        AnalyticsConfiguration(debug = Platform.isDebugBinary)
}
