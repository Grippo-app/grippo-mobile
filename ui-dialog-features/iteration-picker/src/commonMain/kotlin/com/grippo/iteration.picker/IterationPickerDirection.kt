package com.grippo.iteration.picker

import com.grippo.core.models.BaseDirection
import com.grippo.state.trainings.IterationState

public sealed interface IterationPickerDirection : BaseDirection {
    public data class BackWithResult(val value: IterationState) : IterationPickerDirection
    public data object Back : IterationPickerDirection
}
