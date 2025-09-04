package com.grippo.training.recording

import androidx.compose.runtime.Immutable
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Immutable
internal data class TrainingRecordingState(
    val tab: RecordingTab = RecordingTab.Exercises,
    val exercises: ImmutableList<ExerciseState> = persistentListOf(),
    val examples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    val muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
    val startAt: LocalDateTime = DateTimeUtils.now(),

    // === Basic metrics chips ===
    val totalVolume: VolumeFormatState = VolumeFormatState.of(0f),
    val totalRepetitions: RepetitionsFormatState = RepetitionsFormatState.of(0),
    val averageIntensity: IntensityFormatState = IntensityFormatState.of(0f),

    // === Exercise volume bar ===
    val exerciseVolumeData: Pair<DSBarData, Instruction?> = DSBarData(items = emptyList()) to null,

    // === Muscle analysis ===
    val muscleLoadData: Pair<DSProgressData, Instruction?> = DSProgressData(items = emptyList()) to null,

    // === Exercise example circles ===
    val categoryDistributionData: Pair<DSPieData, Instruction?> = DSPieData(slices = emptyList()) to null,
    val weightTypeDistributionData: Pair<DSPieData, Instruction?> = DSPieData(slices = emptyList()) to null,
    val forceTypeDistributionData: Pair<DSPieData, Instruction?> = DSPieData(slices = emptyList()) to null,
    val experienceDistributionData: Pair<DSPieData, Instruction?> = DSPieData(slices = emptyList()) to null,

    // === Exercise volume bar ===
    val intraProgressionData: Pair<DSAreaData, Instruction?> = DSAreaData(points = emptyList()) to null,
    val percent1RMData: Pair<DSAreaData, Instruction?> = DSAreaData(points = emptyList()) to null,
    val stimulusData: Pair<DSAreaData, Instruction?> = DSAreaData(points = emptyList()) to null,
    val estimated1RMData: Pair<DSBarData, Instruction?> = DSBarData(items = emptyList()) to null,
)

@Immutable
internal enum class RecordingTab {
    Exercises,
    Stats
}
