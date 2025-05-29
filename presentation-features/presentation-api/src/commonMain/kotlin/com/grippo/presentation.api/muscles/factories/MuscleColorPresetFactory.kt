package com.grippo.presentation.api.muscles.factories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.icons.muscles.MuscleColorPreset
import com.grippo.presentation.api.muscles.models.MuscleEnumState
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import com.grippo.presentation.api.muscles.models.MuscleState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

public object MuscleColorPresetFactory {

    @Composable
    public fun fromSelected(
        selected: ImmutableSet<MuscleEnumState>
    ): MuscleColorPreset {
        val selectedColor = AppTokens.colors.muscle.active
        val inactiveColor = AppTokens.colors.muscle.inactive

        val colorMap = remember(selected) {
            MuscleEnumState.entries.associateWith { muscle ->
                if (muscle in selected) selectedColor else inactiveColor
            }
        }

        return MuscleColorPreset(
            biceps = colorMap[MuscleEnumState.BICEPS] ?: inactiveColor,
            triceps = colorMap[MuscleEnumState.TRICEPS] ?: inactiveColor,
            forearm = colorMap[MuscleEnumState.FOREARM] ?: inactiveColor,
            forearmFront = colorMap[MuscleEnumState.FOREARM] ?: inactiveColor,
            forearmBack = colorMap[MuscleEnumState.FOREARM] ?: inactiveColor,

            lateralDeltoid = colorMap[MuscleEnumState.LATERAL_DELTOID] ?: inactiveColor,
            anteriorDeltoid = colorMap[MuscleEnumState.ANTERIOR_DELTOID] ?: inactiveColor,
            posteriorDeltoid = colorMap[MuscleEnumState.POSTERIOR_DELTOID] ?: inactiveColor,

            pectoralisMajorAbdominal = colorMap[MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL]
                ?: inactiveColor,
            pectoralisMajorClavicular = colorMap[MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR]
                ?: inactiveColor,
            pectoralisMajorSternocostal = colorMap[MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL]
                ?: inactiveColor,

            rectusAbdominis = colorMap[MuscleEnumState.RECTUS_ABDOMINIS] ?: inactiveColor,
            obliquesAbdominis = colorMap[MuscleEnumState.OBLIQUES] ?: inactiveColor,

            rhomboids = colorMap[MuscleEnumState.RHOMBOIDS] ?: inactiveColor,
            latissimus = colorMap[MuscleEnumState.LATISSIMUS_DORSI] ?: inactiveColor,
            trapezius = colorMap[MuscleEnumState.TRAPEZIUS] ?: inactiveColor,
            teresMajor = colorMap[MuscleEnumState.TERES_MAJOR] ?: inactiveColor,

            gluteal = colorMap[MuscleEnumState.GLUTEAL] ?: inactiveColor,
            hamstrings = colorMap[MuscleEnumState.HAMSTRINGS] ?: inactiveColor,
            calf = colorMap[MuscleEnumState.CALF] ?: inactiveColor,
            quadriceps = colorMap[MuscleEnumState.QUADRICEPS] ?: inactiveColor,
            adductors = colorMap[MuscleEnumState.ADDUCTORS] ?: inactiveColor,
            abductors = colorMap[MuscleEnumState.ABDUCTORS] ?: inactiveColor,

            other = inactiveColor,
            outline = AppTokens.colors.muscle.outline,
            backgroundFront = AppTokens.colors.muscle.background,
            backgroundBack = AppTokens.colors.muscle.background
        )
    }

    @Composable
    public fun fromSelected(
        group: MuscleGroupState<*>,
        selected: ImmutableSet<String>
    ): MuscleColorPreset {

        val muscles: ImmutableSet<MuscleState> = remember(group.muscles) {
            group.muscles.map { it.value }.toImmutableSet()
        }

        val colorMap: Map<MuscleEnumState, Color> = resolveColor(muscles, selected)

        return MuscleColorPreset(
            biceps = colorMap[MuscleEnumState.BICEPS] ?: AppTokens.colors.muscle.inactive,
            triceps = colorMap[MuscleEnumState.TRICEPS] ?: AppTokens.colors.muscle.inactive,
            forearm = colorMap[MuscleEnumState.FOREARM] ?: AppTokens.colors.muscle.inactive,
            forearmFront = colorMap[MuscleEnumState.FOREARM] ?: AppTokens.colors.muscle.inactive,
            forearmBack = colorMap[MuscleEnumState.FOREARM] ?: AppTokens.colors.muscle.inactive,

            lateralDeltoid = colorMap[MuscleEnumState.LATERAL_DELTOID]
                ?: AppTokens.colors.muscle.inactive,
            anteriorDeltoid = colorMap[MuscleEnumState.ANTERIOR_DELTOID]
                ?: AppTokens.colors.muscle.inactive,
            posteriorDeltoid = colorMap[MuscleEnumState.POSTERIOR_DELTOID]
                ?: AppTokens.colors.muscle.inactive,

            pectoralisMajorAbdominal = colorMap[MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL]
                ?: AppTokens.colors.muscle.inactive,
            pectoralisMajorClavicular = colorMap[MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR]
                ?: AppTokens.colors.muscle.inactive,
            pectoralisMajorSternocostal = colorMap[MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL]
                ?: AppTokens.colors.muscle.inactive,

            rectusAbdominis = colorMap[MuscleEnumState.RECTUS_ABDOMINIS]
                ?: AppTokens.colors.muscle.inactive,
            obliquesAbdominis = colorMap[MuscleEnumState.OBLIQUES]
                ?: AppTokens.colors.muscle.inactive,

            rhomboids = colorMap[MuscleEnumState.RHOMBOIDS] ?: AppTokens.colors.muscle.inactive,
            latissimus = colorMap[MuscleEnumState.LATISSIMUS_DORSI]
                ?: AppTokens.colors.muscle.inactive,
            trapezius = colorMap[MuscleEnumState.TRAPEZIUS] ?: AppTokens.colors.muscle.inactive,
            teresMajor = colorMap[MuscleEnumState.TERES_MAJOR] ?: AppTokens.colors.muscle.inactive,

            gluteal = colorMap[MuscleEnumState.GLUTEAL] ?: AppTokens.colors.muscle.inactive,
            hamstrings = colorMap[MuscleEnumState.HAMSTRINGS] ?: AppTokens.colors.muscle.inactive,
            calf = colorMap[MuscleEnumState.CALF] ?: AppTokens.colors.muscle.inactive,
            quadriceps = colorMap[MuscleEnumState.QUADRICEPS] ?: AppTokens.colors.muscle.inactive,
            adductors = colorMap[MuscleEnumState.ADDUCTORS] ?: AppTokens.colors.muscle.inactive,
            abductors = colorMap[MuscleEnumState.ABDUCTORS] ?: AppTokens.colors.muscle.inactive,

            other = AppTokens.colors.muscle.inactive,
            outline = AppTokens.colors.muscle.outline,
            backgroundFront = AppTokens.colors.muscle.background,
            backgroundBack = AppTokens.colors.muscle.background
        )
    }

    @Composable
    private fun resolveColor(
        muscles: ImmutableSet<MuscleState>,
        selectedIds: ImmutableSet<String>
    ): Map<MuscleEnumState, Color> {
        val selectedColor = AppTokens.colors.muscle.active
        val inactiveColor = AppTokens.colors.muscle.inactive

        return remember(muscles, selectedIds) {
            muscles.associate { muscle ->
                muscle.type to if (muscle.id in selectedIds) selectedColor else inactiveColor
            }
        }
    }
}