package com.grippo.home.home.components

import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DensityFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.engagement.TrainingStreakMood
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightGoodFrequency
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightNearBest
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightNeedsAttention
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightProgressWin
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.UserStatsState
import com.grippo.core.state.profile.stubUser
import com.grippo.core.state.trainings.stubTraining
import com.grippo.home.home.HomeState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.minutes

internal fun homePreviewState(
    lifetimeTrainingCount: Int = 12,
    hasGoal: Boolean = true,
): HomeState {
    val date = DateTimeFormatState.of(
        value = LocalDateTime(2026, 10, 15, 10, 0),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    val range = DateRangeFormatState.Valid(
        display = "Sep 16 – Oct 15",
        value = DateRange(
            from = LocalDateTime(2026, 9, 16, 0, 0),
            to = LocalDateTime(2026, 10, 15, 23, 59),
        ),
        kind = DateRangeKind.Last30Days,
    )
    val total = TrainingTotalState(
        repetitions = RepetitionsFormatState.of(84),
        intensity = IntensityFormatState.of(36f),
        volume = VolumeFormatState.of(3_240f),
    )
    val training = stubTraining().let { stub ->
        stub.copy(
            id = "home-preview-training",
            createdAt = date,
            duration = DurationFormatState.of(47.minutes),
            total = total,
            exercises = stub.exercises.mapIndexed { index, exercise ->
                exercise.copy(
                    id = "home-preview-exercise-$index",
                    createdAt = date,
                    total = total,
                    iterations = persistentListOf(),
                    exerciseExample = exercise.exerciseExample.copy(
                        id = "home-preview-example-$index",
                        lastUsed = date,
                    ),
                )
            }.toPersistentList(),
        )
    }
    val muscleLoad = stubMuscleLoadSummary().let { stub ->
        stub.copy(
            meta = stub.meta.copy(
                trainingsCount = lifetimeTrainingCount,
                totalExercises = 6 * lifetimeTrainingCount,
                totalSets = 18 * lifetimeTrainingCount,
                totalRepetitions = 84 * lifetimeTrainingCount,
                totalVolume = 3_240f * lifetimeTrainingCount,
                dominantGroup = MuscleGroupEnumState.CHEST_MUSCLES,
            ),
        )
    }
    val goalProgress = stubGoalProgress(sessionCount = lifetimeTrainingCount).let { stub ->
        stub.copy(
            now = date,
            goal = stub.goal.copy(
                primaryGoal = GoalPrimaryGoalEnumState.GET_STRONGER,
                secondaryGoal = GoalSecondaryGoalEnumState.CONSISTENCY,
                createdAt = DateTimeFormatState.of(
                    value = LocalDateTime(2026, 9, 27, 10, 0),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                ),
                target = DateTimeFormatState.of(
                    value = LocalDateTime(2026, 12, 26, 10, 0),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                ),
                personalizations = persistentListOf(),
            ),
        )
    }
    val streak = stubTrainingStreaks().first().let { stub ->
        stub.copy(
            totalActiveDays = lifetimeTrainingCount.coerceIn(1, 9),
            featured = TrainingStreakFeaturedState.Daily(
                length = lifetimeTrainingCount.coerceIn(1, 5),
                mood = TrainingStreakMood.OnTrack,
                progressPercent = 60,
                confidence = 0.8f,
            ),
            timeline = stub.timeline.map { it.copy(range = range) }.toPersistentList(),
        )
    }
    val spotlights = persistentListOf(
        stubExerciseSpotlightNeedsAttention(),
        stubExerciseSpotlightProgressWin(),
        stubExerciseSpotlightGoodFrequency(),
        stubExerciseSpotlightNearBest(),
    ).mapIndexed { index, spotlight ->
        spotlight.copy(
            example = spotlight.example.copy(
                id = "home-preview-spotlight-$index",
                lastUsed = date,
            ),
        )
    }.toPersistentList()

    return HomeState(
        range = range,
        user = stubUser().copy(
            id = "home-preview-user",
            height = HeightFormatState.of(178),
            weight = WeightFormatState.of(78f),
            createdAt = date,
            stats = UserStatsState(
                trainingsCount = lifetimeTrainingCount,
                totalDuration = DurationFormatState.of((47 * lifetimeTrainingCount).minutes),
                totalVolume = VolumeFormatState.of(3_240f * lifetimeTrainingCount),
                totalRepetitions = RepetitionsFormatState.of(84 * lifetimeTrainingCount),
            ),
        ),
        lastTraining = training,
        muscleLoad = muscleLoad,
        streak = streak,
        performance = stubPerformanceMetrics().distinctBy { it.type }.toPersistentList(),
        goalProgress = if (hasGoal) goalProgress else null,
        spotlights = spotlights,
        excludedMusclesCount = 3,
        missingEquipmentCount = 5,
        hasGoal = hasGoal,
    )
}

internal fun homePreviewEmptyPerformance(): ImmutableList<PerformanceMetricState> =
    stubPerformanceMetrics().distinctBy { it.type }.map { metric ->
        when (metric) {
            is PerformanceMetricState.Duration -> metric.copy(
                deltaPercentage = 0,
                currentVsAveragePercentage = 0,
                current = DurationFormatState.of(0.minutes),
                average = DurationFormatState.of(0.minutes),
                best = DurationFormatState.of(0.minutes),
                status = PerformanceTrendStatusState.Empty,
            )

            is PerformanceMetricState.Volume -> metric.copy(
                deltaPercentage = 0,
                currentVsAveragePercentage = 0,
                current = VolumeFormatState.of(0f),
                average = VolumeFormatState.of(0f),
                best = VolumeFormatState.of(0f),
                status = PerformanceTrendStatusState.Empty,
            )

            is PerformanceMetricState.Density -> metric.copy(
                deltaPercentage = 0,
                currentVsAveragePercentage = 0,
                current = DensityFormatState.of(0f),
                average = DensityFormatState.of(0f),
                best = DensityFormatState.of(0f),
                status = PerformanceTrendStatusState.Empty,
            )

            is PerformanceMetricState.Repetitions -> metric.copy(
                deltaPercentage = 0,
                currentVsAveragePercentage = 0,
                current = RepetitionsFormatState.of(0),
                average = RepetitionsFormatState.of(0),
                best = RepetitionsFormatState.of(0),
                status = PerformanceTrendStatusState.Empty,
            )

            is PerformanceMetricState.Intensity -> metric.copy(
                deltaPercentage = 0,
                currentVsAveragePercentage = 0,
                current = IntensityFormatState.of(0f),
                average = IntensityFormatState.of(0f),
                best = IntensityFormatState.of(0f),
                status = PerformanceTrendStatusState.Empty,
            )
        }
    }.toPersistentList()
