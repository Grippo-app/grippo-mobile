package com.grippo.core.foundation.analytics

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

/** Bind once at the leaf. Each RESUMED activation presents a screen; pause/destroy closes it. */
public interface ScreenAnalytics {
    public fun bind(lifecycle: Lifecycle, present: () -> ScreenPresentation)
}
