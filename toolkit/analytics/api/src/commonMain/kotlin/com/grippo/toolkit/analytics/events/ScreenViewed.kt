package com.grippo.toolkit.analytics.events

import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

public class ScreenViewed(public val screen: ScreenId) : AnalyticsEvent {
    override val schema: AnalyticsEventSchema = Schema
    override fun parameters(): List<AnalyticsParameter> = listOf(Screen.value(screen))

    public companion object {
        private val Screen: AnalyticsParameterKey<ScreenId> = AnalyticsParameterKey.code("screen_name")
        public val Schema: AnalyticsEventSchema = AnalyticsEventSchema("screen_view", 1, listOf(Screen))
    }
}
