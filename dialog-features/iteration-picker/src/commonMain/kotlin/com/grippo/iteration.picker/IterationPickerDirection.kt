package com.grippo.iteration.picker

import com.grippo.core.models.BaseDirection

public sealed interface IterationPickerDirection : BaseDirection {
    public data class BackWithResult(val weight: Float, val repetitions: Int) :
        IterationPickerDirection

    public data object Back : IterationPickerDirection
}




