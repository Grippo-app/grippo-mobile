package com.grippo.core.foundation.analytics

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation

/**
 * Register once during construction of a leaf component, never in Render or on a container.
 * Each RESUMED activation (including foreground return) is a new screen view.
 * The navigation owner supplies the actual reason/referrer for each activation; nothing is inferred.
 * Use the component's child lifecycle and keep its restored FlowContext outside this binding.
 */
public fun Lifecycle.trackScreenViews(
    analytics: Analytics,
    navigation: () -> NavigationContext,
    flow: () -> FlowContext?,
) {
    check(state != Lifecycle.State.DESTROYED) { "Cannot track a destroyed screen" }
    subscribe(object : Lifecycle.Callbacks {
        private var presentation: ScreenPresentation? = null

        override fun onResume() {
            presentation = analytics.presentScreen(navigation(), flow())
        }

        override fun onPause() {
            val active = presentation
            presentation = null
            active?.close()
        }

        override fun onDestroy() {
            onPause()
            unsubscribe(this)
        }
    })
}
