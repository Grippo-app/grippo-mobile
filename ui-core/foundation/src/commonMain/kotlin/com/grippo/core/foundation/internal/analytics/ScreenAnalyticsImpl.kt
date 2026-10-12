package com.grippo.core.foundation.internal.analytics

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.grippo.core.foundation.analytics.ScreenAnalytics
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import org.koin.core.annotation.Single

/** Stateless singleton; each binding owns its callback and presentation independently. */
@Single(binds = [ScreenAnalytics::class])
internal class ScreenAnalyticsImpl : ScreenAnalytics {

    override fun bind(lifecycle: Lifecycle, present: () -> ScreenPresentation) {
        check(lifecycle.state != Lifecycle.State.DESTROYED) { "Cannot track a destroyed screen" }

        lifecycle.subscribe(object : Lifecycle.Callbacks {
            private var presentation: ScreenPresentation? = null

            override fun onResume() {
                presentation = try {
                    present()
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
