package com.grippo.analytics.iteration.picker.internal.schema

import com.grippo.analytics.common.models.InputStatus
import com.grippo.analytics.iteration.picker.models.IterationPickerEvent
import com.grippo.analytics.iteration.picker.models.IterationPickerField
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object IterationPickerEventMatrix {
    private val Field = AnalyticsParameterKey.code<IterationPickerField>("input_field", encode = {
        when (it) {
            IterationPickerField.ExternalWeight -> "external_weight"
            IterationPickerField.AssistWeight -> "assist_weight"
            IterationPickerField.ExtraWeight -> "extra_weight"
            IterationPickerField.Repetitions -> "repetitions"
        }
    })
    private val Status = AnalyticsParameterKey.code<InputStatus>("input_status", encode = {
        when (it) {
            InputStatus.Empty -> "empty"
            InputStatus.Invalid -> "invalid"
            InputStatus.Valid -> "valid"
        }
    })
    private val InputChanged = AnalyticsEventSchema("iteration_picker_input_changed", 1, listOf(Field, Status))
    private val BodyWeightOpened = AnalyticsEventSchema("iteration_picker_body_weight_opened", 1, emptyList())
    private val BodyWeightConfirmed = AnalyticsEventSchema("iteration_picker_body_weight_confirmed", 1, listOf(Status))
    private val PreviousSelected = AnalyticsEventSchema("iteration_picker_previous_selected", 1, emptyList())
    private val IterationSelected = AnalyticsEventSchema("iteration_picker_suggestion_selected", 1, emptyList())
    private val RepetitionSelected = AnalyticsEventSchema("iteration_picker_repetition_selected", 1, emptyList())
    private val Submit = AnalyticsEventSchema("iteration_picker_submit", 1, emptyList())
    private val Back = AnalyticsEventSchema("iteration_picker_back", 1, emptyList())

    val all = listOf(InputChanged, BodyWeightOpened, BodyWeightConfirmed, PreviousSelected, IterationSelected, RepetitionSelected, Submit, Back)

    fun event(event: IterationPickerEvent): AnalyticsEvent = when (event) {
        is IterationPickerEvent.InputChanged -> Payload(InputChanged, listOf(Field.value(event.field), Status.value(event.status)))
        IterationPickerEvent.BodyWeightOpened -> Payload(BodyWeightOpened)
        is IterationPickerEvent.BodyWeightConfirmed -> Payload(BodyWeightConfirmed, listOf(Status.value(event.status)))
        IterationPickerEvent.PreviousIterationSelected -> Payload(PreviousSelected)
        IterationPickerEvent.IterationSuggestionSelected -> Payload(IterationSelected)
        IterationPickerEvent.RepetitionSuggestionSelected -> Payload(RepetitionSelected)
        IterationPickerEvent.Submit -> Payload(Submit)
        IterationPickerEvent.Back -> Payload(Back)
    }

    private class Payload(
        override val schema: AnalyticsEventSchema,
        private val parameters: List<AnalyticsParameter> = emptyList(),
    ) : AnalyticsEvent {
        override fun parameters(): List<AnalyticsParameter> = parameters
    }
}
