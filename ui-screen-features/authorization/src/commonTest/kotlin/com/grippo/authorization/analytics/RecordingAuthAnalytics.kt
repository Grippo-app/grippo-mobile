package com.grippo.authorization.analytics

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.*
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.transport.AnalyticsValue

internal class RecordingAuthAnalytics : Analytics {
    data class Captured(val event: AnalyticsEvent, val flow: FlowContext?, val operation: OperationId?) {
        fun text(key: String): String? = (event.parameters().singleOrNull { it.key.name == key }?.value as? AnalyticsValue.Text)?.value
    }
    data class View(val navigation: NavigationContext, val flow: FlowContext?)
    val events = mutableListOf<Captured>()
    val views = mutableListOf<View>()
    val names get() = events.map { it.event.schema.name }
    override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
        events += Captured(event, flow, operation)
    }
    override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation {
        views += View(navigation, flow)
        return object : ScreenPresentation { override fun close() = Unit }
    }
    override fun updateContext(context: AnalyticsContext) = Unit
    override fun updateIdentity(identity: AnalyticsIdentity) = Unit
    override fun updateCollection(collection: AnalyticsCollection) = Unit
}
