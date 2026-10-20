package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_streak_details_active_days
import com.grippo.design.resources.provider.training_streak_details_no_sequence
import kotlinx.datetime.LocalDateTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

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

    @Composable
    public fun activeDaysDescription(): String = AppTokens.strings.res(
        Res.plurals.training_streak_details_active_days,
        totalActiveDays,
        totalActiveDays,
    )

    @Composable
    public fun summaryHeadline(): String = if (featured.length == 0) {
        activeDaysDescription()
    } else {
        featured.detailsHeadline()
    }

    @Composable
    public fun summaryDescription(): String = if (featured.length == 0) {
        AppTokens.strings.res(Res.string.training_streak_details_no_sequence)
    } else {
        featured.cadenceDescription()
    }
}

public fun stubTrainingStreaks(): ImmutableList<TrainingStreakState> {
    val values = persistentListOf(
        TrainingStreakState(
            totalActiveDays = 9,
            featured = TrainingStreakFeaturedState.Daily(
                length = 5,
                mood = TrainingStreakMood.CrushingIt,
                progressPercent = 100,
                confidence = 0.8f,
            ),
            timeline = persistentListOf(
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 1,
                    targetTrainingDays = 1,
                    isPartial = false,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 16, 0, 0),
                        to = LocalDateTime(2026, 10, 16, 23, 59),
                    ),
                ),
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 1,
                    targetTrainingDays = 1,
                    isPartial = false,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 17, 0, 0),
                        to = LocalDateTime(2026, 10, 17, 23, 59),
                    ),
                ),
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 1,
                    targetTrainingDays = 1,
                    isPartial = true,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 18, 0, 0),
                        to = LocalDateTime(2026, 10, 18, 23, 59),
                    ),
                ),
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
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 4,
                    targetTrainingDays = 4,
                    isPartial = false,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 5, 0, 0),
                        to = LocalDateTime(2026, 10, 11, 23, 59),
                    ),
                ),
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 4,
                    targetTrainingDays = 4,
                    isPartial = false,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 12, 0, 0),
                        to = LocalDateTime(2026, 10, 18, 23, 59),
                    ),
                ),
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 1,
                    targetTrainingDays = 4,
                    isPartial = true,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 19, 0, 0),
                        to = LocalDateTime(2026, 10, 25, 23, 59),
                    ),
                ),
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
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 2,
                    targetTrainingDays = 2,
                    isPartial = false,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 10, 0, 0),
                        to = LocalDateTime(2026, 10, 12, 23, 59),
                    ),
                ),
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 2,
                    targetTrainingDays = 2,
                    isPartial = false,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 13, 0, 0),
                        to = LocalDateTime(2026, 10, 15, 23, 59),
                    ),
                ),
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 2,
                    targetTrainingDays = 2,
                    isPartial = false,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 16, 0, 0),
                        to = LocalDateTime(2026, 10, 18, 23, 59),
                    ),
                ),
                stubTrainingStreakProgress().copy(
                    achievedTrainingDays = 1,
                    targetTrainingDays = 2,
                    isPartial = true,
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 19, 0, 0),
                        to = LocalDateTime(2026, 10, 21, 23, 59),
                    ),
                ),
            ),
            historyDays = 45,
            lastTrainingGapDays = 0,
        ),
    )
    val pattern = TrainingStreakFeaturedState.Pattern(
        length = 12,
        mask = persistentListOf(true, false, true, false, true, false, false, false),
        mood = TrainingStreakMood.OnTrack,
        progressPercent = 67,
        confidence = 0.8f,
    )
    val patternValue = values.first().copy(
        totalActiveDays = 128,
        featured = pattern,
        historyDays = 365,
        timeline = persistentListOf(),
    )
    return (values + patternValue + patternValue.copy(
        totalActiveDays = 0,
        featured = pattern.copy(
            length = 0,
            mood = TrainingStreakMood.Restart,
            progressPercent = 0,
            confidence = 0.5f,
        ),
        lastTrainingGapDays = 7,
    )).toPersistentList()
}
