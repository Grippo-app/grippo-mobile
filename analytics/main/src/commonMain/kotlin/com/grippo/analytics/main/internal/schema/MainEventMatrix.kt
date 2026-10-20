package com.grippo.analytics.main.internal.schema

import com.grippo.analytics.main.models.MainEvent
import com.grippo.analytics.main.models.MainTab
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object MainEventMatrix {
    private val Tab = AnalyticsParameterKey.code<MainTab>("main_tab") {
        when (it) {
            MainTab.Home -> "home"
            MainTab.Calendar -> "calendar"
            MainTab.Library -> "library"
            MainTab.Profile -> "profile"
        }
    }
    private val TabSelected = AnalyticsEventSchema("main_tab_selected", 1, listOf(Tab))
    private val StartTrainingClicked = AnalyticsEventSchema("main_start_training_clicked", 1, emptyList())
    val all = listOf(TabSelected, StartTrainingClicked)

    fun event(event: MainEvent): AnalyticsEvent = when (event) {
        is MainEvent.TabSelected -> Payload(TabSelected, listOf(Tab.value(event.tab)))
        MainEvent.StartTrainingClicked -> Payload(StartTrainingClicked, emptyList())
    }

    private class Payload(
        override val schema: AnalyticsEventSchema,
        private val values: List<AnalyticsParameter>,
    ) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = values
    }
}
