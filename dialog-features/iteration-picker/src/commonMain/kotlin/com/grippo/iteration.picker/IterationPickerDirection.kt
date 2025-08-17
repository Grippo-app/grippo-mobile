package com.grippo.iteration.picker

import com.grippo.core.models.BaseDirection

public sealed interface IterationPickerDirection : BaseDirection {
    public data class BackWithResult(
        val volume: Float,
        val repetitions: Int
    ) : IterationPickerDirection

    public data object Back : IterationPickerDirection
}
