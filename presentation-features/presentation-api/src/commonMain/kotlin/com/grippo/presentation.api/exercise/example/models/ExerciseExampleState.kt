package com.grippo.presentation.api.exercise.example.models

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.equipment.models.EquipmentState
import com.grippo.presentation.api.user.models.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class ExerciseExample(
    val id: String,
    val bundles: ImmutableList<ExerciseExampleBundleState>,
    val name: String,
    val imageUrl: String?,
    val description: String,
    val equipments: ImmutableList<EquipmentState>,
    val tutorials: ImmutableList<TutorialState>,
    val experience: ExperienceEnumState,
    val forceType: ForceTypeEnumState,
    val weightType: WeightTypeEnumState,
    val category: CategoryEnumState,
)