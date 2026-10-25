package com.grippo.core.foundation.internal.analytics

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.grippo.core.foundation.analytics.ScreenAnalytics
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import org.koin.core.annotation.Single

@Single(binds = [ScreenAnalytics::class])
internal class ScreenAnalyticsImpl : ScreenAnalytics {

    override fun bind(lifecycle: Lifecycle, present: (NavigationReason) -> ScreenPresentation) {
        check(lifecycle.state != Lifecycle.State.DESTROYED) { "Cannot track a destroyed screen" }

        lifecycle.subscribe(object : Lifecycle.Callbacks {
            private var hasPresented = false
            private var presentation: ScreenPresentation? = null

            override fun onResume() {
                val reason = if (hasPresented) NavigationReason.Restore else NavigationReason.Initial
                hasPresented = true
                presentation = try {
                    present(reason)
                } catch (_: Exception) {
                    null
                }
            }

            override fun onPause() {
                val active = presentation
                presentation = null
                try {
                    active?.close()
                } catch (_: Exception) {
                    // Telemetry must not interrupt lifecycle delivery.
                }
            }

            override fun onDestroy() {
                onPause()
                lifecycle.unsubscribe(this)
            }
        })
    }
}
