package com.grippo.state.exercise.examples

import androidx.compose.runtime.Immutable
import com.grippo.state.filters.FilterValue
import com.grippo.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Immutable
@Serializable
public data class ExerciseExampleValueState(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val description: String,
    val experience: ExperienceEnumState,
    val forceType: ForceTypeEnumState,
    val weightType: WeightTypeEnumState,
    val category: CategoryEnumState,
) {
    public companion object {
        public val filters: ImmutableList<FilterValue> = persistentListOf(
            FilterValue.Category(value = null),
            FilterValue.WeightType(value = null),
            FilterValue.ForceType(value = null)
            // todo add ExperienceEnumState
        )
    }
}


public fun stubExerciseExampleValueState(): ExerciseExampleValueState {
    return ExerciseExampleValueState(
        id = Uuid.random().toString(),
        name = "Dumbbell Bench press",
        imageUrl = null,
        description = "The dumbbell bench press is a variation of the barbell bench press and an exercise used to build the muscles of the chest. It is recommended after reaching a certain point of strength on the barbell bench press to avoid pec and shoulder injuries. The exercise requires maintaining shoulder stability throughout, making it a great test of strength and control.",
        experience = ExperienceEnumState.INTERMEDIATE,
        forceType = ForceTypeEnumState.PUSH,
        weightType = WeightTypeEnumState.BODY_WEIGHT,
        category = CategoryEnumState.COMPOUND
    )
}