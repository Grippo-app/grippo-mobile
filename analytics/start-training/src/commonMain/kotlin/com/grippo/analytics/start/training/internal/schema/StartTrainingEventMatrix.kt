package com.grippo.analytics.start.training.internal.schema

import com.grippo.analytics.start.training.internal.events.StartTrainingPayloadEvent
import com.grippo.analytics.start.training.models.StartTrainingEvent
import com.grippo.analytics.start.training.models.StartTrainingOptionKind
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object StartTrainingEventMatrix {
    private val Option = AnalyticsParameterKey.code<StartTrainingOptionKind>("option") {
        when (it) {
            StartTrainingOptionKind.Empty -> "empty"
            StartTrainingOptionKind.Preset -> "preset"
            StartTrainingOptionKind.Recent -> "recent"
        }
    }
    private val OptionConfirmed = AnalyticsEventSchema("start_training_option_confirmed", 1, listOf(Option))
    private val DismissRequested = AnalyticsEventSchema("start_training_dismiss_requested", 1, emptyList())
    val all = listOf(OptionConfirmed, DismissRequested)

    fun event(event: StartTrainingEvent): AnalyticsEvent = when (event) {
        is StartTrainingEvent.OptionConfirmed -> StartTrainingPayloadEvent(
            OptionConfirmed,
            listOf(Option.value(event.option)),
        )
        StartTrainingEvent.DismissRequested -> StartTrainingPayloadEvent(DismissRequested, emptyList())
    }
}
