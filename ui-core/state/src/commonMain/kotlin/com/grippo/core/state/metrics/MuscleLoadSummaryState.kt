package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class MuscleLoadSummaryState(
    val perGroup: MuscleLoadBreakdownState,
    val perMuscle: MuscleLoadBreakdownState,
    val volumePerGroup: MuscleLoadBreakdownState,
    val volumePerMuscle: MuscleLoadBreakdownState,
    val dominance: MuscleLoadDominanceState,
)

@Immutable
public data class MuscleLoadBreakdownState(
    val entries: List<MuscleLoadEntryState>,
)

@Immutable
public data class MuscleLoadEntryState(
    val group: MuscleGroupEnumState,
    val value: Float,
    val muscles: ImmutableList<MuscleEnumState>,
)

@Immutable
public data class MuscleLoadDominanceState(
    val top1SharePercent: Float,
    val top2SharePercent: Float,
)

public fun stubMuscleLoadSummary(): MuscleLoadSummaryState {
    return MuscleLoadSummaryState(
        perGroup = MuscleLoadBreakdownState(
            entries = listOf(
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.CHEST_MUSCLES,
                    value = 42f,
                    muscles = persistentListOf(
                        MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                        MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL
                    )
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.BACK_MUSCLES,
                    value = 27f,
                    muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI)
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.ARMS_AND_FOREARMS,
                    value = 18f,
                    muscles = persistentListOf(MuscleEnumState.BICEPS, MuscleEnumState.TRICEPS)
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.LEGS,
                    value = 13f,
                    muscles = persistentListOf(
                        MuscleEnumState.QUADRICEPS,
                        MuscleEnumState.HAMSTRINGS
                    )
                )
            )
        ),
        perMuscle = MuscleLoadBreakdownState(
            entries = listOf(
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.CHEST_MUSCLES,
                    value = 52f,
                    muscles = persistentListOf(MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR)
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.BACK_MUSCLES,
                    value = 34f,
                    muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI)
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.ARMS_AND_FOREARMS,
                    value = 21f,
                    muscles = persistentListOf(MuscleEnumState.BICEPS)
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.LEGS,
                    value = 18f,
                    muscles = persistentListOf(MuscleEnumState.QUADRICEPS)
                ),
            )
        ),
        volumePerGroup = MuscleLoadBreakdownState(entries = emptyList()),
        volumePerMuscle = MuscleLoadBreakdownState(entries = emptyList()),
        dominance = MuscleLoadDominanceState(
            top1SharePercent = 52f,
            top2SharePercent = 74f,
        ),
    )
}
