package com.grippo.analytics.language.picker.internal.schema

import com.grippo.analytics.language.picker.models.LanguagePickerEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal object LanguagePickerEventMatrix {
    private val LanguageClicked = AnalyticsEventSchema("language_picker_language_selected", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("language_picker_dismiss_requested", 1, emptyList())

    val all = listOf(LanguageClicked, DismissRequested)

    fun event(event: LanguagePickerEvent): AnalyticsEvent = Payload(
        when (event) {
            LanguagePickerEvent.LanguageClicked -> LanguageClicked
            LanguagePickerEvent.DismissRequested -> DismissRequested
        }
    )

    private class Payload(override val schema: AnalyticsEventSchema) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = emptyList()
    }
}
