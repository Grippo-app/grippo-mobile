package com.grippo.core.state.trainings.highlight

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Immutable
public data class Highlight(
    val totalDuration: Duration,
    val focusExercise: HighlightExerciseFocus?,
    val muscleFocus: HighlightMuscleFocus?,
    val consistency: HighlightConsistency,
    val performance: List<HighlightPerformanceMetric>,
)

@Immutable
public data class HighlightExerciseFocus(
    val name: String,
    val sessions: Int,
    val totalVolume: VolumeFormatState,
    val forceType: ForceTypeEnumState,
    val weightType: WeightTypeEnumState,
)

@Immutable
public data class HighlightMuscleFocus(
    val muscleGroup: MuscleGroupEnumState,
    val load: PercentageFormatState,
)

@Immutable
public data class HighlightConsistency(
    val activeDays: Int,
    val bestStreakDays: Int,
)

@Immutable
public enum class HighlightMetric {
    Duration,
    Volume,
    Repetitions,
    Intensity,
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
    Record, Improved, Stable, Declined
}

public fun stubHighlight(): Highlight = Highlight(
    totalDuration = 28.hours,
    focusExercise = HighlightExerciseFocus(
        name = "Bench press",
        sessions = 5,
        totalVolume = VolumeFormatState.of(1_200f),
        forceType = ForceTypeEnumState.PUSH,
        weightType = WeightTypeEnumState.FREE
    ),
    muscleFocus = HighlightMuscleFocus(
        muscleGroup = MuscleGroupEnumState.CHEST_MUSCLES,
        load = PercentageFormatState.of(42)
    ),
    consistency = HighlightConsistency(
        activeDays = 16,
        bestStreakDays = 5,
    ),
    performance = listOf<HighlightPerformanceMetric>(
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
