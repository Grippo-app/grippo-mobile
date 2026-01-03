package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class MuscleLoadSummaryState(
    val perGroup: MuscleLoadBreakdownState,
    val perMuscle: MuscleLoadBreakdownState,
)

@Immutable
public data class MuscleLoadBreakdownState(
    val entries: List<MuscleLoadEntryState>,
)

@Immutable
public data class MuscleLoadEntryState(
    val label: String,
    val value: Float,
    val muscles: ImmutableList<MuscleEnumState>,
)

public fun stubMuscleLoadSummary(): MuscleLoadSummaryState {
    return MuscleLoadSummaryState(
        perGroup = MuscleLoadBreakdownState(
            entries = listOf(
                MuscleLoadEntryState(
                    label = "Chest",
                    value = 42f,
                    muscles = persistentListOf(
                        MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                        MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL
                    )
                ),
                MuscleLoadEntryState(
                    label = "Back",
                    value = 27f,
                    muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI)
                ),
                MuscleLoadEntryState(
                    label = "Arms",
                    value = 18f,
                    muscles = persistentListOf(MuscleEnumState.BICEPS, MuscleEnumState.TRICEPS)
                ),
                MuscleLoadEntryState(
                    label = "Legs",
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
                    label = "Pec Major (Clav.)",
                    value = 52f,
                    muscles = persistentListOf(MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR)
                ),
                MuscleLoadEntryState(
                    label = "Latissimus",
                    value = 34f,
                    muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI)
                ),
                MuscleLoadEntryState(
                    label = "Biceps",
                    value = 21f,
                    muscles = persistentListOf(MuscleEnumState.BICEPS)
                ),
                MuscleLoadEntryState(
                    label = "Quads",
                    value = 18f,
                    muscles = persistentListOf(MuscleEnumState.QUADRICEPS)
                ),
            )
        ),
    )
}
