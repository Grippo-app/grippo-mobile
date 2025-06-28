package com.grippo.presentation.api.muscles.factories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.AppColor
import com.grippo.design.resources.icons.muscles.MuscleColorPreset
import com.grippo.presentation.api.muscles.models.MuscleEnumState
import com.grippo.presentation.api.muscles.models.MuscleEnumState.ABDUCTORS
import com.grippo.presentation.api.muscles.models.MuscleEnumState.ADDUCTORS
import com.grippo.presentation.api.muscles.models.MuscleEnumState.ANTERIOR_DELTOID
import com.grippo.presentation.api.muscles.models.MuscleEnumState.BICEPS
import com.grippo.presentation.api.muscles.models.MuscleEnumState.CALF
import com.grippo.presentation.api.muscles.models.MuscleEnumState.FOREARM
import com.grippo.presentation.api.muscles.models.MuscleEnumState.GLUTEAL
import com.grippo.presentation.api.muscles.models.MuscleEnumState.HAMSTRINGS
import com.grippo.presentation.api.muscles.models.MuscleEnumState.LATERAL_DELTOID
import com.grippo.presentation.api.muscles.models.MuscleEnumState.LATISSIMUS_DORSI
import com.grippo.presentation.api.muscles.models.MuscleEnumState.OBLIQUES
import com.grippo.presentation.api.muscles.models.MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL
import com.grippo.presentation.api.muscles.models.MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR
import com.grippo.presentation.api.muscles.models.MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL
import com.grippo.presentation.api.muscles.models.MuscleEnumState.POSTERIOR_DELTOID
import com.grippo.presentation.api.muscles.models.MuscleEnumState.QUADRICEPS
import com.grippo.presentation.api.muscles.models.MuscleEnumState.RECTUS_ABDOMINIS
import com.grippo.presentation.api.muscles.models.MuscleEnumState.RHOMBOIDS
import com.grippo.presentation.api.muscles.models.MuscleEnumState.TERES_MAJOR
import com.grippo.presentation.api.muscles.models.MuscleEnumState.TRAPEZIUS
import com.grippo.presentation.api.muscles.models.MuscleEnumState.TRICEPS
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

internal object MuscleColorPresetFactory {

    sealed interface MuscleColorMode {
        data object Monochrome : MuscleColorMode
        data class Colorful(val color: AppColor.MuscleColors.Colorful) : MuscleColorMode
    }

    sealed class MuscleColorSelection {
        data class EnumSelection(
            val selected: ImmutableSet<MuscleEnumState>,
            val mode: MuscleColorMode
        ) : MuscleColorSelection()

        data class GroupSelection(
            val group: MuscleGroupState<*>,
            val selectedIds: ImmutableSet<String>,
        ) : MuscleColorSelection()
    }

    @Composable
    fun from(selection: MuscleColorSelection): MuscleColorPreset {
        return when (selection) {
            is MuscleColorSelection.EnumSelection -> {
                when (selection.mode) {
                    MuscleColorMode.Monochrome -> createMonochromePreset(
                        selection.selected
                    )

                    is MuscleColorMode.Colorful -> createColorfulPreset(
                        selection.selected,
                        selection.mode.color
                    )
                }
            }

            is MuscleColorSelection.GroupSelection -> {
                val muscles = remember(selection.group.muscles) {
                    selection.group.muscles.map { it.value }.toImmutableSet()
                }

                val active = AppTokens.colors.muscle.active
                val inactive = AppTokens.colors.muscle.inactive

                val colorMap = remember(muscles, selection.selectedIds) {
                    muscles.associate {
                        it.type to if (it.id in selection.selectedIds) active else inactive
                    }
                }

                buildPreset(colorMap)
            }
        }
    }

    @Composable
    private fun createColorfulPreset(
        selected: ImmutableSet<MuscleEnumState>,
        colors: AppColor.MuscleColors.Colorful
    ): MuscleColorPreset {
        val inactiveColor = AppTokens.colors.muscle.inactive

        val colorMap = remember(colors, selected) {
            MuscleEnumState.entries.associateWith { muscle ->
                if (muscle in selected) muscle.allocateColorByMuscle(colors) else inactiveColor
            }
        }

        return buildPreset(colorMap)
    }

    @Composable
    private fun createMonochromePreset(
        selected: ImmutableSet<MuscleEnumState>,
    ): MuscleColorPreset {
        val selectedColor = AppTokens.colors.muscle.active
        val inactiveColor = AppTokens.colors.muscle.inactive

        val colorMap = remember(selected) {
            MuscleEnumState.entries.associateWith { muscle ->
                if (muscle in selected) selectedColor else inactiveColor
            }
        }

        return buildPreset(colorMap)
    }

    @Composable
    private fun buildPreset(colorMap: Map<MuscleEnumState, Color>): MuscleColorPreset {
        val inactive = AppTokens.colors.muscle.inactive
        val get = { muscle: MuscleEnumState -> colorMap[muscle] ?: inactive }

        return MuscleColorPreset(
            biceps = get(BICEPS),
            triceps = get(TRICEPS),
            forearm = get(FOREARM),
            forearmFront = get(FOREARM),
            forearmBack = get(FOREARM),

            lateralDeltoid = get(LATERAL_DELTOID),
            anteriorDeltoid = get(ANTERIOR_DELTOID),
            posteriorDeltoid = get(POSTERIOR_DELTOID),

            pectoralisMajorAbdominal = get(PECTORALIS_MAJOR_ABDOMINAL),
            pectoralisMajorClavicular = get(PECTORALIS_MAJOR_CLAVICULAR),
            pectoralisMajorSternocostal = get(PECTORALIS_MAJOR_STERNOCOSTAL),

            rectusAbdominis = get(RECTUS_ABDOMINIS),
            obliquesAbdominis = get(OBLIQUES),

            rhomboids = get(RHOMBOIDS),
            latissimus = get(LATISSIMUS_DORSI),
            trapezius = get(TRAPEZIUS),
            teresMajor = get(TERES_MAJOR),

            gluteal = get(GLUTEAL),
            hamstrings = get(HAMSTRINGS),
            calf = get(CALF),
            quadriceps = get(QUADRICEPS),
            adductors = get(ADDUCTORS),
            abductors = get(ABDUCTORS),

            other = inactive,
            outline = AppTokens.colors.muscle.outline,
            backgroundFront = AppTokens.colors.muscle.background,
            backgroundBack = AppTokens.colors.muscle.background
        )
    }
}