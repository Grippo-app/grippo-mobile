package com.grippo.presentation.api.exercise.example.models

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.equipment.models.EquipmentState
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class ExerciseExampleState(
    val value: ExerciseExampleValueState,
    val bundles: ImmutableList<ExerciseExampleBundleState>,
    val equipments: ImmutableList<EquipmentState>,
    val tutorials: ImmutableList<TutorialState>,
)