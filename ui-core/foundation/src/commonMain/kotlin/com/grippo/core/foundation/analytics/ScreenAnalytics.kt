package com.grippo.core.foundation.analytics

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

public interface ScreenAnalytics {
    public fun bind(lifecycle: Lifecycle, present: (NavigationReason) -> ScreenPresentation)
}
