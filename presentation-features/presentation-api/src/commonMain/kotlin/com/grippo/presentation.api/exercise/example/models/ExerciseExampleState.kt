package com.grippo.presentation.api.exercise.example.models

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.equipment.models.EquipmentState
import com.grippo.presentation.api.equipment.models.stubEquipments
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class ExerciseExampleState(
    val value: ExerciseExampleValueState,
    val bundles: ImmutableList<ExerciseExampleBundleState>,
    val equipments: ImmutableList<EquipmentState>,
    val tutorials: ImmutableList<TutorialState>,
)

public fun stubExerciseExample(): ExerciseExampleState {
    return ExerciseExampleState(
        value = stubExerciseExampleValueState(),
        bundles = persistentListOf(),
        equipments = stubEquipments().random().equipments.take(2).toPersistentList(),
        tutorials = persistentListOf(stubTutorial())
    )
}