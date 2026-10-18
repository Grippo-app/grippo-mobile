package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import kotlinx.datetime.LocalDateTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TrainingStreakState(
    val totalActiveDays: Int,
    val featured: TrainingStreakFeaturedState,
    val timeline: ImmutableList<TrainingStreakProgressState>,
    val historyDays: Int,
    val lastTrainingGapDays: Int,
) {
    public val kind: TrainingStreakKind
        get() = when (featured) {
            is TrainingStreakFeaturedState.Daily -> TrainingStreakKind.Daily
            is TrainingStreakFeaturedState.Weekly -> TrainingStreakKind.Weekly
            is TrainingStreakFeaturedState.Rhythm -> TrainingStreakKind.Rhythm
            is TrainingStreakFeaturedState.Pattern -> TrainingStreakKind.Pattern
        }

    public val score: Float
        get() = featured.confidence
}

public fun stubTrainingStreaks(): ImmutableList<TrainingStreakState> {
    return persistentListOf(
        TrainingStreakState(
            totalActiveDays = 9,
            featured = TrainingStreakFeaturedState.Daily(
                length = 5,
                mood = TrainingStreakMood.CrushingIt,
                progressPercent = 100,
                confidence = 0.8f,
            ),
            timeline = persistentListOf(
                streakPreviewPeriod(fromDay = 16, toDay = 16, achieved = 1, target = 1),
                streakPreviewPeriod(fromDay = 17, toDay = 17, achieved = 1, target = 1),
                streakPreviewPeriod(fromDay = 18, toDay = 18, achieved = 1, target = 1, isPartial = true),
            ),
            historyDays = 30,
            lastTrainingGapDays = 0,
        ),
        TrainingStreakState(
            totalActiveDays = 14,
            featured = TrainingStreakFeaturedState.Weekly(
                length = 2,
                targetTrainingDaysPerPeriod = 4,
                mood = TrainingStreakMood.OnTrack,
                progressPercent = 25,
                confidence = 0.7f,
            ),
            timeline = persistentListOf(
                streakPreviewPeriod(fromDay = 5, toDay = 11, achieved = 4, target = 4),
                streakPreviewPeriod(fromDay = 12, toDay = 18, achieved = 4, target = 4),
                streakPreviewPeriod(fromDay = 19, toDay = 25, achieved = 1, target = 4, isPartial = true),
            ),
            historyDays = 60,
            lastTrainingGapDays = 1,
        ),
        TrainingStreakState(
            totalActiveDays = 16,
            featured = TrainingStreakFeaturedState.Rhythm(
                length = 3,
                workDays = 2,
                restDays = 1,
                mood = TrainingStreakMood.OnTrack,
                progressPercent = 50,
                confidence = 0.9f,
            ),
            timeline = persistentListOf(
                streakPreviewPeriod(fromDay = 10, toDay = 12, achieved = 2, target = 2),
                streakPreviewPeriod(fromDay = 13, toDay = 15, achieved = 2, target = 2),
                streakPreviewPeriod(fromDay = 16, toDay = 18, achieved = 2, target = 2),
                streakPreviewPeriod(fromDay = 19, toDay = 21, achieved = 1, target = 2, isPartial = true),
            ),
            historyDays = 45,
            lastTrainingGapDays = 0,
        ),
    )
}

private fun streakPreviewPeriod(
    fromDay: Int,
    toDay: Int,
    achieved: Int,
    target: Int,
    isPartial: Boolean = false,
): TrainingStreakProgressState {
    return TrainingStreakProgressState(
        achievedTrainingDays = achieved,
        targetTrainingDays = target,
        isPartial = isPartial,
        range = DateRangeFormatState.of(
            from = LocalDateTime(2026, 10, fromDay, 0, 0),
            to = LocalDateTime(2026, 10, toDay, 23, 59),
        ),
    )
}
