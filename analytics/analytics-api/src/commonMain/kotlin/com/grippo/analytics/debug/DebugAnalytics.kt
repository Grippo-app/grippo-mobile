package com.grippo.analytics.debug

import com.grippo.analytics.debug.models.DebugScreen
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

public interface DebugAnalytics {
    public fun presentScreen(screen: DebugScreen, reason: NavigationReason): ScreenPresentation
}
