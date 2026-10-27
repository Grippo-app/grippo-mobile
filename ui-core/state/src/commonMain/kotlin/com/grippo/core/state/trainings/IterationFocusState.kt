package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class IterationFocusState {
    VOLUME,
    REPETITIONS,
    UNIDENTIFIED
}
