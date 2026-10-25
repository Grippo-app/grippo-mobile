package com.grippo.training.completed

import androidx.compose.runtime.Immutable

@Immutable
internal enum class RecordCelebrationState {
    Pending,
    Playing,
    Done;

    fun celebrate(isRecord: Boolean): RecordCelebrationState {
        return if (this == Pending && isRecord) Playing else this
    }
}
