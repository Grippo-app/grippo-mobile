package com.grippo.toolkit.analytics.core.internal

import org.koin.core.scope.Scope

internal actual fun createAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration =
    AnalyticsConfiguration(debug = Platform.isDebugBinary)
