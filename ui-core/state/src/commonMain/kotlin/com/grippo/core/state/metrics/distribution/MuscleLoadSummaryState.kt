package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class MuscleLoadSummaryState(
    val meta: MuscleLoadMetaState,
    val perGroup: MuscleLoadBreakdownState,
    val perMuscle: MuscleLoadBreakdownState,
    val volumePerGroup: MuscleLoadBreakdownState,
    val volumePerMuscle: MuscleLoadBreakdownState,
    val dominance: MuscleLoadDominanceState,
    val groupDominance: MuscleLoadDominanceState,
)

public fun stubMuscleLoadSummary(): MuscleLoadSummaryState {
    val perGroup = MuscleLoadBreakdownState(
        entries = persistentListOf(
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.CHEST_MUSCLES,
                value = 42f,
                muscles = persistentListOf(
                    MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                    MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL,
                ),
                hitTrainingsCount = 12,
                primaryTrainingsCount = 6,
                avgWeightedSetsPerSession = 5.04f,
                maxWeightedSetsInOneSession = 10f,
                avgVolumePerHitSession = 840f,
                maxVolumeInOneSession = 1500f,
                topExampleIds = persistentListOf("bench_press", "incline_bench_press"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.BACK_MUSCLES,
                value = 27f,
                muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI),
                hitTrainingsCount = 10,
                primaryTrainingsCount = 3,
                avgWeightedSetsPerSession = 3.888f,
                maxWeightedSetsInOneSession = 8f,
                avgVolumePerHitSession = 648f,
                maxVolumeInOneSession = 1200f,
                topExampleIds = persistentListOf("pull_up"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.ARMS_AND_FOREARMS,
                value = 18f,
                muscles = persistentListOf(MuscleEnumState.BICEPS, MuscleEnumState.TRICEPS),
                hitTrainingsCount = 9,
                primaryTrainingsCount = 2,
                avgWeightedSetsPerSession = 2.88f,
                maxWeightedSetsInOneSession = 6f,
                avgVolumePerHitSession = 480f,
                maxVolumeInOneSession = 900f,
                topExampleIds = persistentListOf("cable_curl"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.LEGS,
                value = 13f,
                muscles = persistentListOf(MuscleEnumState.HAMSTRINGS, MuscleEnumState.QUADRICEPS),
                hitTrainingsCount = 7,
                primaryTrainingsCount = 1,
                avgWeightedSetsPerSession = 2.6742857f,
                maxWeightedSetsInOneSession = 6f,
                avgVolumePerHitSession = 445.7143f,
                maxVolumeInOneSession = 800f,
                topExampleIds = persistentListOf("squat"),
            ),
        ),
    )
    val perMuscle = MuscleLoadBreakdownState(
        entries = persistentListOf(
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.BACK_MUSCLES,
                value = 27f,
                muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI),
                hitTrainingsCount = 10,
                avgWeightedSetsPerSession = 3.888f,
                maxWeightedSetsInOneSession = 8f,
                avgVolumePerHitSession = 648f,
                maxVolumeInOneSession = 1200f,
                topExampleIds = persistentListOf("pull_up"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.CHEST_MUSCLES,
                value = 24f,
                muscles = persistentListOf(MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR),
                hitTrainingsCount = 12,
                avgWeightedSetsPerSession = 2.88f,
                maxWeightedSetsInOneSession = 6f,
                avgVolumePerHitSession = 480f,
                maxVolumeInOneSession = 850f,
                topExampleIds = persistentListOf("incline_bench_press"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.CHEST_MUSCLES,
                value = 18f,
                muscles = persistentListOf(MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL),
                hitTrainingsCount = 12,
                avgWeightedSetsPerSession = 2.16f,
                maxWeightedSetsInOneSession = 4f,
                avgVolumePerHitSession = 360f,
                maxVolumeInOneSession = 650f,
                topExampleIds = persistentListOf("bench_press"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.ARMS_AND_FOREARMS,
                value = 10f,
                muscles = persistentListOf(MuscleEnumState.BICEPS),
                hitTrainingsCount = 9,
                avgWeightedSetsPerSession = 1.6f,
                maxWeightedSetsInOneSession = 3.5f,
                avgVolumePerHitSession = 266.6667f,
                maxVolumeInOneSession = 500f,
                topExampleIds = persistentListOf("cable_curl"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.LEGS,
                value = 8f,
                muscles = persistentListOf(MuscleEnumState.QUADRICEPS),
                hitTrainingsCount = 7,
                avgWeightedSetsPerSession = 1.6457143f,
                maxWeightedSetsInOneSession = 3.5f,
                avgVolumePerHitSession = 274.2857f,
                maxVolumeInOneSession = 500f,
                topExampleIds = persistentListOf("squat"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.ARMS_AND_FOREARMS,
                value = 8f,
                muscles = persistentListOf(MuscleEnumState.TRICEPS),
                hitTrainingsCount = 9,
                avgWeightedSetsPerSession = 1.28f,
                maxWeightedSetsInOneSession = 2.5f,
                avgVolumePerHitSession = 213.3333f,
                maxVolumeInOneSession = 400f,
                topExampleIds = persistentListOf("bench_press"),
            ),
            MuscleLoadEntryState(
                group = MuscleGroupEnumState.LEGS,
                value = 5f,
                muscles = persistentListOf(MuscleEnumState.HAMSTRINGS),
                hitTrainingsCount = 7,
                avgWeightedSetsPerSession = 1.0285714f,
                maxWeightedSetsInOneSession = 2.5f,
                avgVolumePerHitSession = 171.4286f,
                maxVolumeInOneSession = 300f,
                topExampleIds = persistentListOf("squat"),
            ),
        ),
    )

    return MuscleLoadSummaryState(
        meta = MuscleLoadMetaState(
            trainingsCount = 12,
            totalExercises = 48,
            totalSets = 144,
            totalRepetitions = 1440,
            totalVolume = 24000f,
            dominantGroup = MuscleGroupEnumState.CHEST_MUSCLES,
        ),
        perGroup = perGroup,
        perMuscle = perMuscle,
        volumePerGroup = perGroup,
        volumePerMuscle = perMuscle,
        dominance = MuscleLoadDominanceState(
            top1SharePercent = 27f,
            top2SharePercent = 51f,
        ),
        groupDominance = MuscleLoadDominanceState(
            top1SharePercent = 42f,
            top2SharePercent = 69f,
        ),
    )
}
