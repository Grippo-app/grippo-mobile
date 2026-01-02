package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Immutable
public data class HighlightState(
    val totalDuration: Duration,
    val spotlight: ExerciseSpotlight?,
    val muscleLoad: MuscleLoadSummary?,
    val streak: TrainingStreakState,
    val performance: List<PerformanceMetricState>,
)

public fun stubHighlight(): HighlightState = HighlightState(
    totalDuration = 28.hours,
    spotlight = stubExerciseSpotlight(),
    muscleLoad = stubMuscleLoadSummary(),
    streak = stubTrainingStreakStates().random(),
    performance = stubPerformanceMetrics()
)
