package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class IterationFocus {
    VOLUME,
    REPETITIONS,
    UNIDENTIFIED,
}