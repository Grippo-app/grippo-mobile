package com.grippo.core.state.examples

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime
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
    val usageCount: Int,
    val lastUsed: LocalDateTime?,
)

public fun stubExerciseExampleValueState(): ExerciseExampleValueState {
    return ExerciseExampleValueState(
        id = Uuid.random().toString(),
        name = "Dumbbell Bench press",
        imageUrl = null,
        description = "The dumbbell bench press is a variation of the barbell bench press and an exercise used to build the muscles of the chest. It is recommended after reaching a certain point of strength on the barbell bench press to avoid pec and shoulder injuries. The exercise requires maintaining shoulder stability throughout, making it a great test of strength and control.",
        experience = ExperienceEnumState.INTERMEDIATE,
        forceType = ForceTypeEnumState.PUSH,
        weightType = WeightTypeEnumState.BODY_WEIGHT,
        category = CategoryEnumState.COMPOUND,
        usageCount = 4,
        lastUsed = DateTimeUtils.now()
    )
}
