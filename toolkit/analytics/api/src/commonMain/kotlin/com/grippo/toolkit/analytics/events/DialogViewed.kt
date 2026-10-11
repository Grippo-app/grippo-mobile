package com.grippo.toolkit.analytics.events

import com.grippo.toolkit.analytics.context.DialogId
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

public class DialogViewed(public val dialog: DialogId) : AnalyticsEvent {
    override val schema: AnalyticsEventSchema = Schema
    override fun parameters(): List<AnalyticsParameter> = listOf(Dialog.value(dialog))

    public companion object {
        private val Dialog: AnalyticsParameterKey<DialogId> = AnalyticsParameterKey.code("dialog_name")
        public val Schema: AnalyticsEventSchema = AnalyticsEventSchema("dialog_viewed", 1, listOf(Dialog))
    }
}
