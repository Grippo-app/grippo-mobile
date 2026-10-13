package com.grippo.analytics.profile

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.schema.AnalyticsEvent

internal class RecordingProfileAnalytics : Analytics {
    val views = mutableListOf<Pair<NavigationContext, FlowContext?>>()

    override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation {
        views += navigation to flow
        return object : ScreenPresentation {
            override fun close() = Unit
        }
    }

    override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?): Unit =
        error("Profile screen presentation must not emit custom events")

    override fun updateContext(context: AnalyticsContext): Unit = error("Unexpected context update")
    override fun updateIdentity(identity: AnalyticsIdentity): Unit = error("Unexpected identity update")
    override fun updateCollection(collection: AnalyticsCollection): Unit = error("Unexpected collection update")
}
