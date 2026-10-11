package com.grippo.shared

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.AnalyticsSink
import com.grippo.toolkit.analytics.core.DefaultAnalytics
import org.koin.mp.KoinPlatformTools

/** Platform bootstrap only. Features receive Analytics from Koin. */
public object AnalyticsRuntime {
    public val analytics: Analytics
        get() = instance()

    public fun attachSink(sink: AnalyticsSink) {
        instance().attachSink(sink)
    }

    public fun diagnosticMessages(): List<String> = instance().diagnostics().map {
        listOfNotNull(it.disposition.name, it.eventName).joinToString(": ")
    }

    private fun instance(): DefaultAnalytics = KoinPlatformTools.defaultContext().get().get()
}
