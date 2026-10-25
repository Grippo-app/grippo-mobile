package com.grippo.toolkit.analytics.core.internal

import org.koin.core.scope.Scope

internal class AnalyticsConfiguration(val debug: Boolean)

internal expect fun createAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration
