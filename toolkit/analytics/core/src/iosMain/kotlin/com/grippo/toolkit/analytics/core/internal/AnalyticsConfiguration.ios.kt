package com.grippo.toolkit.analytics.core.internal

import kotlin.experimental.ExperimentalNativeApi
import org.koin.core.scope.Scope

@OptIn(ExperimentalNativeApi::class)
internal actual fun createAnalyticsConfiguration(scope: Scope): AnalyticsConfiguration =
    AnalyticsConfiguration(debug = Platform.isDebugBinary)
