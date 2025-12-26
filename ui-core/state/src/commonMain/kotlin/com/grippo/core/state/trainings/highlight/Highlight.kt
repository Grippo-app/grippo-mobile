package com.grippo.core.state.trainings.highlight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.icons.Intensity
import com.grippo.design.resources.provider.icons.Repeat
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.icons.Volume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Immutable
public data class Highlight(
    val totalDuration: Duration,
    val focusExercise: ExerciseExampleState?,
    val muscleFocus: HighlightMuscleFocus?,
    val streak: HighlightStreak,
    val performance: List<HighlightPerformanceMetric>,
)

@Immutable
public data class HighlightMuscleFocus(
    val segments: List<HighlightMuscleFocusSegment>,
)

@Immutable
public data class HighlightMuscleFocusSegment(
    val muscleGroup: MuscleGroupEnumState,
    val load: PercentageFormatState,
)

@Immutable
public data class HighlightStreak(
    val totalActiveDays: Int,
    val featured: HighlightStreakFeatured,
    val timeline: List<HighlightStreakProgressEntry>,
)

@Immutable
public data class HighlightStreakFeatured(
    val type: HighlightStreakType,
    val length: Int,
    val targetSessionsPerPeriod: Int,
    val periodLengthDays: Int,
    val mood: HighlightStreakMood,
    val progressPercent: Int,
    val rhythm: HighlightStreakRhythm? = null,
)

@Immutable
public enum class HighlightStreakType {
    Daily,
    Weekly,
    Rhythm,
}

@Immutable
public enum class HighlightStreakMood {
    CrushingIt,
    OnTrack,
    Restart,
}

@Immutable
public data class HighlightStreakProgressEntry(
    val progressPercent: Int,
    val achievedSessions: Int,
    val targetSessions: Int,
)

@Immutable
public data class HighlightStreakRhythm(
    val workDays: Int,
    val restDays: Int,
)

@Immutable
public enum class HighlightMetric {
    Duration,
    Volume,
    Repetitions,
    Intensity;

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            Duration -> AppTokens.icons.Timer
            Volume -> AppTokens.icons.Volume
            Repetitions -> AppTokens.icons.Repeat
            Intensity -> AppTokens.icons.Intensity
        }
    }
}

@Immutable
public sealed interface HighlightPerformanceMetric {
    public val metric: HighlightMetric
    public val deltaPercentage: Int
    public val status: HighlightPerformanceStatus

    @Immutable
    public data class Volume(
        override val deltaPercentage: Int,
        val current: VolumeFormatState,
        val average: VolumeFormatState,
        val best: VolumeFormatState,
        override val status: HighlightPerformanceStatus,
    ) : HighlightPerformanceMetric {
        override val metric: HighlightMetric = HighlightMetric.Volume
    }

    @Immutable
    public data class Duration(
        override val deltaPercentage: Int,
        val current: kotlin.time.Duration,
        val average: kotlin.time.Duration,
        val best: kotlin.time.Duration,
        override val status: HighlightPerformanceStatus,
    ) : HighlightPerformanceMetric {
        override val metric: HighlightMetric = HighlightMetric.Duration
    }

    @Immutable
    public data class Repetitions(
        override val deltaPercentage: Int,
        val current: RepetitionsFormatState,
        val average: RepetitionsFormatState,
        val best: RepetitionsFormatState,
        override val status: HighlightPerformanceStatus,
    ) : HighlightPerformanceMetric {
        override val metric: HighlightMetric = HighlightMetric.Repetitions
    }

    @Immutable
    public data class Intensity(
        override val deltaPercentage: Int,
        val current: IntensityFormatState,
        val average: IntensityFormatState,
        val best: IntensityFormatState,
        override val status: HighlightPerformanceStatus,
    ) : HighlightPerformanceMetric {
        override val metric: HighlightMetric = HighlightMetric.Intensity
    }
}

@Immutable
public enum class HighlightPerformanceStatus {
    Record,
    Improved,
    Stable,
    Declined
}

public fun stubHighlight(): Highlight = Highlight(
    totalDuration = 28.hours,
    focusExercise = stubExerciseExample(),
    muscleFocus = HighlightMuscleFocus(
        segments = listOf(
            HighlightMuscleFocusSegment(
                muscleGroup = MuscleGroupEnumState.CHEST_MUSCLES,
                load = PercentageFormatState.of(42)
            ),
            HighlightMuscleFocusSegment(
                muscleGroup = MuscleGroupEnumState.BACK_MUSCLES,
                load = PercentageFormatState.of(27)
            ),
            HighlightMuscleFocusSegment(
                muscleGroup = MuscleGroupEnumState.ARMS_AND_FOREARMS,
                load = PercentageFormatState.of(18)
            ),
            HighlightMuscleFocusSegment(
                muscleGroup = MuscleGroupEnumState.LEGS,
                load = PercentageFormatState.of(13)
            )
        )
    ),
    streak = HighlightStreak(
        totalActiveDays = 16,
        featured = HighlightStreakFeatured(
            type = HighlightStreakType.Rhythm,
            length = 4,
            targetSessionsPerPeriod = 2,
            periodLengthDays = 3,
            mood = HighlightStreakMood.CrushingIt,
            progressPercent = 80,
            rhythm = HighlightStreakRhythm(workDays = 2, restDays = 1),
        ),
        timeline = listOf(
            HighlightStreakProgressEntry(
                progressPercent = 100,
                achievedSessions = 2,
                targetSessions = 2
            ),
            HighlightStreakProgressEntry(
                progressPercent = 100,
                achievedSessions = 2,
                targetSessions = 2
            ),
            HighlightStreakProgressEntry(
                progressPercent = 60,
                achievedSessions = 1,
                targetSessions = 2
            ),
            HighlightStreakProgressEntry(
                progressPercent = 80,
                achievedSessions = 2,
                targetSessions = 2
            ),
        )
    ),
    performance = listOf(
        HighlightPerformanceMetric.Volume(
            deltaPercentage = 24,
            current = VolumeFormatState.of(1_200f),
            average = VolumeFormatState.of(900f),
            best = VolumeFormatState.of(1_200f),
            status = HighlightPerformanceStatus.Record
        ),
        HighlightPerformanceMetric.Duration(
            deltaPercentage = -8,
            current = 65.minutes,
            average = 70.minutes,
            best = 75.minutes,
            status = HighlightPerformanceStatus.Declined
        ),
        HighlightPerformanceMetric.Repetitions(
            deltaPercentage = 12,
            current = RepetitionsFormatState.of(84),
            average = RepetitionsFormatState.of(75),
            best = RepetitionsFormatState.of(92),
            status = HighlightPerformanceStatus.Improved
        ),
        HighlightPerformanceMetric.Intensity(
            deltaPercentage = 3,
            current = IntensityFormatState.of(36f),
            average = IntensityFormatState.of(35f),
            best = IntensityFormatState.of(42f),
            status = HighlightPerformanceStatus.Stable
        )
    )
)
