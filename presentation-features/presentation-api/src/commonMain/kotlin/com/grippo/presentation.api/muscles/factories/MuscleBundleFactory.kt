package com.grippo.presentation.api.muscles.factories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.resources.AppColor
import com.grippo.design.resources.icons.muscles.fullBack
import com.grippo.design.resources.icons.muscles.fullFront
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleBundleState
import com.grippo.presentation.api.muscles.factories.MuscleColorPresetFactory.MuscleColorMode
import com.grippo.presentation.api.muscles.models.MuscleEnumState
import com.grippo.presentation.api.muscles.models.MuscleState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableSet

@Composable
public fun ImmutableList<ExerciseExampleBundleState>.images(colors: AppColor.MuscleColors.Colorful): Pair<ImageVector?, ImageVector?> {
    val muscles: List<MuscleState> = map { it.muscle }
    val muscleTypes = muscles.map { it.type }.toImmutableSet()

    val preset = MuscleColorPresetFactory.from(
        MuscleColorPresetFactory.MuscleColorSelection.EnumSelection(
            muscleTypes,
            mode = MuscleColorMode.Colorful(colors)
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

@Immutable
private enum class BodySide {
    Front, Back, Both
}

private object MuscleSideFactory {
    fun side(muscle: MuscleEnumState): BodySide = when (muscle) {
        MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
        MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL,
        MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL,
        MuscleEnumState.RECTUS_ABDOMINIS,
        MuscleEnumState.OBLIQUES,
        MuscleEnumState.ANTERIOR_DELTOID,
        MuscleEnumState.LATERAL_DELTOID,
        MuscleEnumState.QUADRICEPS,
        MuscleEnumState.BICEPS -> BodySide.Front

        MuscleEnumState.TRAPEZIUS,
        MuscleEnumState.LATISSIMUS_DORSI,
        MuscleEnumState.RHOMBOIDS,
        MuscleEnumState.TERES_MAJOR,
        MuscleEnumState.POSTERIOR_DELTOID,
        MuscleEnumState.GLUTEAL,
        MuscleEnumState.HAMSTRINGS,
        MuscleEnumState.CALF,
        MuscleEnumState.ADDUCTORS,
        MuscleEnumState.ABDUCTORS,
        MuscleEnumState.TRICEPS -> BodySide.Back

        MuscleEnumState.FOREARM -> BodySide.Both
    }
}