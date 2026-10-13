package com.grippo.analytics.iteration.picker.models

import com.grippo.analytics.common.models.InputStatus

public sealed interface IterationPickerEvent {
    public data class InputChanged(val field: IterationPickerField, val status: InputStatus) : IterationPickerEvent
    public data object BodyWeightOpened : IterationPickerEvent
    public data class BodyWeightConfirmed(val status: InputStatus) : IterationPickerEvent
    public data object PreviousIterationSelected : IterationPickerEvent
    public data object IterationSuggestionSelected : IterationPickerEvent
    public data object RepetitionSuggestionSelected : IterationPickerEvent
    public data object Submit : IterationPickerEvent
    public data object Back : IterationPickerEvent
}
