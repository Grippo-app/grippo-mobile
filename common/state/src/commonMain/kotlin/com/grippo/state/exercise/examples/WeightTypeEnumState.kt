package com.grippo.state.exercise.examples

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class WeightTypeEnumState {
    FREE,
    FIXED,
    BODY_WEIGHT;
}