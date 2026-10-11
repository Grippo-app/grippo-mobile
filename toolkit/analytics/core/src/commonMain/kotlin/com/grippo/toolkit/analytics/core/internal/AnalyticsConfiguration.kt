package com.grippo.toolkit.analytics.core.internal

import org.koin.core.scope.Scope

/** Enables fail-fast validation and a bounded diagnostic buffer in debug builds. */
internal class AnalyticsConfiguration(val debug: Boolean)

internal expect fun createAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration
