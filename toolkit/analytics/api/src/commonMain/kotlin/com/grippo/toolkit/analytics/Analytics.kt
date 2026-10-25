package com.grippo.toolkit.analytics

import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.schema.AnalyticsEvent

public interface Analytics {
    public fun track(event: AnalyticsEvent, operation: OperationId? = null)
    public fun presentScreen(navigation: NavigationContext): ScreenPresentation
    public fun updateContext(context: AnalyticsContext)
    public fun updateIdentity(identity: AnalyticsIdentity)
    public fun updateCollection(collection: AnalyticsCollection)
}
