package com.grippo.analytics.draft.training.internal.schema

import com.grippo.analytics.draft.training.internal.events.DraftTrainingPayloadEvent
import com.grippo.analytics.draft.training.models.DraftTrainingEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema

internal object DraftTrainingEventMatrix {
    private val ContinueClicked = AnalyticsEventSchema("draft_training_continue_clicked", 1, emptyList())
    private val StartNewClicked = AnalyticsEventSchema("draft_training_start_new_clicked", 1, emptyList())
    private val DismissRequested = AnalyticsEventSchema("draft_training_dismiss_requested", 1, emptyList())
    val all = listOf(ContinueClicked, StartNewClicked, DismissRequested)

    fun event(event: DraftTrainingEvent): AnalyticsEvent = DraftTrainingPayloadEvent(
        when (event) {
            DraftTrainingEvent.ContinueClicked -> ContinueClicked
            DraftTrainingEvent.StartNewClicked -> StartNewClicked
            DraftTrainingEvent.DismissRequested -> DismissRequested
        },
        emptyList(),
    )
}
