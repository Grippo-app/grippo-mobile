package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable

@Deprecated("don't use it")
@Immutable
public data class TrainingLoadProfileState(
    val kind: TrainingProfileKindState,
    val dimensions: List<TrainingDimensionScoreState>,
)

@Immutable
public enum class TrainingDimensionKindState {
    Strength,
    Hypertrophy,
    Endurance,
}

@Immutable
public enum class TrainingProfileKindState {
    Strength,
    Hypertrophy,
    Endurance,
    Powerbuilding,
    Mixed,
    Easy,
}

@Immutable
public data class TrainingDimensionScoreState(
    val kind: TrainingDimensionKindState,
    val score: Int, // 0..100
)
