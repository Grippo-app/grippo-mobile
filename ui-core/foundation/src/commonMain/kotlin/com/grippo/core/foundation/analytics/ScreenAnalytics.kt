package com.grippo.core.foundation.analytics

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

/** Each resume presents a screen: Initial on first activation, Restore afterwards. Pause/destroy closes it. */
public interface ScreenAnalytics {
    public fun bind(lifecycle: Lifecycle, present: (NavigationReason) -> ScreenPresentation)
}
