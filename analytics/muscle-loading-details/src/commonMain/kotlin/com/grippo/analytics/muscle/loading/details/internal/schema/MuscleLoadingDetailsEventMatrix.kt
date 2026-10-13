package com.grippo.analytics.muscle.loading.details.internal.schema

import com.grippo.analytics.muscle.loading.details.models.MuscleLoadingDetailsEvent
import com.grippo.analytics.muscle.loading.details.models.MuscleLoadingDetailsMode
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object MuscleLoadingDetailsEventMatrix {
    private val Mode = AnalyticsParameterKey.code<MuscleLoadingDetailsMode>("muscle_loading_mode") {
        when (it) {
            MuscleLoadingDetailsMode.PerGroup -> "per_group"
            MuscleLoadingDetailsMode.PerMuscle -> "per_muscle"
        }
    }
    private val ModeSelected = AnalyticsEventSchema("muscle_loading_mode_selected", 1, listOf(Mode))
    private val DismissRequested = AnalyticsEventSchema("muscle_loading_dismiss_requested", 1, emptyList())

    val all = listOf(ModeSelected, DismissRequested)

    fun event(event: MuscleLoadingDetailsEvent): AnalyticsEvent = when (event) {
        is MuscleLoadingDetailsEvent.ModeSelected -> Payload(ModeSelected, listOf(Mode.value(event.mode)))
        MuscleLoadingDetailsEvent.DismissRequested -> Payload(DismissRequested, emptyList())
    }

    private class Payload(
        override val schema: AnalyticsEventSchema,
        private val values: List<AnalyticsParameter>,
    ) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = values
    }
}
