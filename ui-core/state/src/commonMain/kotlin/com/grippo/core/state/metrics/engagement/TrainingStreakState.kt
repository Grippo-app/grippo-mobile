package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.toolkit.date.utils.DateRangeKind

@Immutable
public data class TrainingStreakState(
    val totalActiveDays: Int,
    val featured: TrainingStreakFeaturedState,
    val timeline: List<TrainingStreakProgressState>,
    val kind: TrainingStreakKind,
    val score: Float,
    val historyDays: Int,
    val lastTrainingGapDays: Int,
)

public fun stubTrainingStreaks(): List<TrainingStreakState> {
    return listOf(
        TrainingStreakState(
            totalActiveDays = 9,
            featured = TrainingStreakFeaturedState.Daily(
                length = 5,
                mood = TrainingStreakMood.OnTrack,
                progressPercent = 60,
                confidence = 0.8f,
            ),
            timeline = listOf(
                TrainingStreakProgressState(
                    progressPercent = 100,
                    achievedSessions = 1,
                    targetSessions = 1,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
                TrainingStreakProgressState(
                    progressPercent = 100,
                    achievedSessions = 1,
                    targetSessions = 1,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
                TrainingStreakProgressState(
                    progressPercent = 50,
                    achievedSessions = 1,
                    targetSessions = 2,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
            ),
            kind = TrainingStreakKind.Daily,
            score = 0.72f,
            historyDays = 30,
            lastTrainingGapDays = 0,
        ),
        TrainingStreakState(
            totalActiveDays = 14,
            featured = TrainingStreakFeaturedState.Weekly(
                length = 3,
                targetSessionsPerPeriod = 4,
                mood = TrainingStreakMood.Paused,
                progressPercent = 40,
                confidence = 0.7f,
            ),
            timeline = listOf(
                TrainingStreakProgressState(
                    progressPercent = 75,
                    achievedSessions = 3,
                    targetSessions = 4,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
                TrainingStreakProgressState(
                    progressPercent = 40,
                    achievedSessions = 2,
                    targetSessions = 5,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
                TrainingStreakProgressState(
                    progressPercent = 20,
                    achievedSessions = 1,
                    targetSessions = 5,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
            ),
            kind = TrainingStreakKind.Weekly,
            score = 0.48f,
            historyDays = 60,
            lastTrainingGapDays = 4,
        ),
        TrainingStreakState(
            totalActiveDays = 16,
            featured = TrainingStreakFeaturedState.Rhythm(
                length = 4,
                workDays = 2,
                restDays = 1,
                mood = TrainingStreakMood.CrushingIt,
                progressPercent = 80,
                confidence = 0.9f,
            ),
            timeline = listOf(
                TrainingStreakProgressState(
                    progressPercent = 100,
                    achievedSessions = 2,
                    targetSessions = 2,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
                TrainingStreakProgressState(
                    progressPercent = 100,
                    achievedSessions = 2,
                    targetSessions = 2,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
                TrainingStreakProgressState(
                    progressPercent = 60,
                    achievedSessions = 1,
                    targetSessions = 2,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
                TrainingStreakProgressState(
                    progressPercent = 80,
                    achievedSessions = 2,
                    targetSessions = 2,
                    range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                ),
            ),
            kind = TrainingStreakKind.Rhythm,
            score = 0.88f,
            historyDays = 45,
            lastTrainingGapDays = 1,
        )
    )
}
