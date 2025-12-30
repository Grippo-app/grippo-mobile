package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.muscles.MuscleEnumState
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Immutable
public data class Highlight(
    val totalDuration: Duration,
    val spotlight: ExerciseSpotlight?,
    val muscleLoad: MuscleLoadSummary?,
    val streak: TrainingStreakState,
    val performance: List<PerformanceMetricState>,
)

public fun stubHighlight(): Highlight = Highlight(
    totalDuration = 28.hours,
    spotlight = stubExerciseSpotlight(),
    muscleLoad = MuscleLoadSummary(
        perGroup = MuscleLoadBreakdown(
            entries = listOf(
                MuscleLoadEntry(
                    label = "Chest",
                    value = 42f,
                    muscles = persistentListOf(
                        MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                        MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL
                    )
                ),
                MuscleLoadEntry(
                    label = "Back",
                    value = 27f,
                    muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI)
                ),
                MuscleLoadEntry(
                    label = "Arms",
                    value = 18f,
                    muscles = persistentListOf(MuscleEnumState.BICEPS, MuscleEnumState.TRICEPS)
                ),
                MuscleLoadEntry(
                    label = "Legs",
                    value = 13f,
                    muscles = persistentListOf(
                        MuscleEnumState.QUADRICEPS,
                        MuscleEnumState.HAMSTRINGS
                    )
                )
            )
        ),
        perMuscle = MuscleLoadBreakdown(
            entries = listOf(
                MuscleLoadEntry(
                    label = "Pec Major (Clav.)",
                    value = 52f,
                    muscles = persistentListOf(MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR)
                ),
                MuscleLoadEntry(
                    label = "Latissimus",
                    value = 34f,
                    muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI)
                ),
                MuscleLoadEntry(
                    label = "Biceps",
                    value = 21f,
                    muscles = persistentListOf(MuscleEnumState.BICEPS)
                ),
                MuscleLoadEntry(
                    label = "Quads",
                    value = 18f,
                    muscles = persistentListOf(MuscleEnumState.QUADRICEPS)
                ),
            )
        ),
    ),
    streak = TrainingStreakState(
        totalActiveDays = 16,
        featured = TrainingStreakFeaturedState(
            type = TrainingStreakType.Rhythm,
            length = 4,
            targetSessionsPerPeriod = 2,
            periodLengthDays = 3,
            mood = TrainingStreakMood.CrushingIt,
            progressPercent = 80,
            rhythm = TrainingStreakRhythmState(workDays = 2, restDays = 1),
        ),
        timeline = listOf(
            TrainingStreakProgressState(
                progressPercent = 100,
                achievedSessions = 2,
                targetSessions = 2
            ),
            TrainingStreakProgressState(
                progressPercent = 100,
                achievedSessions = 2,
                targetSessions = 2
            ),
            TrainingStreakProgressState(
                progressPercent = 60,
                achievedSessions = 1,
                targetSessions = 2
            ),
            TrainingStreakProgressState(
                progressPercent = 80,
                achievedSessions = 2,
                targetSessions = 2
            ),
        )
    ),
    performance = listOf(
        PerformanceMetricState.Volume(
            deltaPercentage = 24,
            current = VolumeFormatState.of(1_200f),
            average = VolumeFormatState.of(900f),
            best = VolumeFormatState.of(1_200f),
            status = PerformanceTrendStatusState.Record
        ),
        PerformanceMetricState.Duration(
            deltaPercentage = -8,
            current = 65.minutes,
            average = 70.minutes,
            best = 75.minutes,
            status = PerformanceTrendStatusState.Declined
        ),
        PerformanceMetricState.Repetitions(
            deltaPercentage = 12,
            current = RepetitionsFormatState.of(84),
            average = RepetitionsFormatState.of(75),
            best = RepetitionsFormatState.of(92),
            status = PerformanceTrendStatusState.Improved
        ),
        PerformanceMetricState.Intensity(
            deltaPercentage = 3,
            current = IntensityFormatState.of(36f),
            average = IntensityFormatState.of(35f),
            best = IntensityFormatState.of(42f),
            status = PerformanceTrendStatusState.Stable
        )
    )
)
