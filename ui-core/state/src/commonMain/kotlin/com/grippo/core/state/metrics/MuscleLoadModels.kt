package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class MuscleLoadSummary(
    val perGroup: MuscleLoadBreakdown,
    val perMuscle: MuscleLoadBreakdown,
)

@Immutable
public data class MuscleLoadBreakdown(
    val entries: List<MuscleLoadEntry>,
)

@Immutable
public data class MuscleLoadEntry(
    val label: String,
    val value: Float,
    val muscles: ImmutableList<MuscleEnumState>,
)

public fun stubMuscleLoadSummary(): MuscleLoadSummary {
    return MuscleLoadSummary(
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
    )
}
