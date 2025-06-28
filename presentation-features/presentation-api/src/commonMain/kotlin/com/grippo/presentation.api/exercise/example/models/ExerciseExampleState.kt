package com.grippo.presentation.api.exercise.example.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.resources.icons.muscles.fullBack
import com.grippo.design.resources.icons.muscles.fullFront
import com.grippo.presentation.api.equipment.models.EquipmentState
import com.grippo.presentation.api.equipment.models.stubEquipments
import com.grippo.presentation.api.muscles.factories.BodySide
import com.grippo.presentation.api.muscles.factories.MuscleColorPresetFactory
import com.grippo.presentation.api.muscles.factories.MuscleSideFactory
import com.grippo.presentation.api.muscles.models.MuscleState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class ExerciseExampleState(
    val value: ExerciseExampleValueState,
    val bundles: ImmutableList<ExerciseExampleBundleState>,
    val equipments: ImmutableList<EquipmentState>,
    val tutorials: ImmutableList<TutorialState>,
) {
    @Composable
    public fun image(): Pair<ImageVector?, ImageVector?> {
        val muscles: List<MuscleState> = bundles.map { it.muscle }
        val muscleTypes = muscles.map { it.type }.toImmutableSet()

        val preset = MuscleColorPresetFactory.from(
            MuscleColorPresetFactory.MuscleColorSelection.EnumSelection(
                muscleTypes
            )
        )

        val hasFront = muscleTypes.any {
            val side = MuscleSideFactory.side(it)
            side == BodySide.Front || side == BodySide.Both
        }

        val hasBack = muscleTypes.any {
            val side = MuscleSideFactory.side(it)
            side == BodySide.Back || side == BodySide.Both
        }

        val frontImage = if (hasFront) fullFront(preset) else null
        val backImage = if (hasBack) fullBack(preset) else null

        return frontImage to backImage
    }
}

public fun stubExerciseExample(): ExerciseExampleState {
    return ExerciseExampleState(
        value = stubExerciseExampleValueState(),
        bundles = persistentListOf(
            stubExerciseExampleBundle(),
            stubExerciseExampleBundle()
        ),
        equipments = stubEquipments().random().equipments.take(2).toPersistentList(),
        tutorials = persistentListOf(stubTutorial())
    )
}