package com.grippo.shared.configuration

import android.app.Application
import android.content.pm.ApplicationInfo
import com.grippo.toolkit.analytics.core.AnalyticsConfiguration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
public actual class ApplicationConfigurationModule actual constructor() {
    @Single
    internal actual fun provideAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration {
        val application = scope.get<Application>()
        return AnalyticsConfiguration(debug = application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
    }
}
