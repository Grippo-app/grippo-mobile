package com.grippo.toolkit.analytics.core.internal

import android.app.Application
import android.content.pm.ApplicationInfo
import org.koin.core.scope.Scope

internal actual fun createAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration {
    val application = scope.get<Application>()
    return AnalyticsConfiguration(debug = application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
}
