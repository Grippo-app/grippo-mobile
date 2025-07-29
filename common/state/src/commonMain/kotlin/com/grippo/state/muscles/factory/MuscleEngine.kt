package com.grippo.state.muscles.factory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.icons.muscles.MuscleColorPreset
import com.grippo.design.resources.icons.muscles.fullBack
import com.grippo.design.resources.icons.muscles.fullFront
import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

public object MuscleEngine {

    @Composable
    public fun generatePreset(strategy: MuscleColorStrategy): MuscleColorPreset {
        return when (strategy) {
            is MuscleColorStrategy.Monochrome -> generateMonochrome()

            is MuscleColorStrategy.ByAlpha -> generateByAlpha(
                strategy.bundles
            )

            is MuscleColorStrategy.BySelection -> generateFromSelection(
                strategy.group,
                strategy.selectedIds
            )

            is MuscleColorStrategy.ByUniqueColor -> generateByUniqueColor(
                strategy.bundles
            )
        }
    }

    @Composable
    public fun generateImages(
        preset: MuscleColorPreset,
        bundles: ImmutableList<ExerciseExampleBundleState>
    ): Pair<ImageVector, ImageVector> {
        return images(bundles, preset)
    }

    @Composable
    private fun generateMonochrome(): MuscleColorPreset {
        val selectedColor = AppTokens.colors.muscle.active
        val inactiveColor = AppTokens.colors.muscle.inactive

        val colorMap = MuscleEnumState.entries.associateWith { selectedColor }
        return buildPreset(colorMap, inactiveColor)
    }

    @Composable
    private fun generateByAlpha(bundles: ImmutableList<ExerciseExampleBundleState>): MuscleColorPreset {
        val focusedColor = AppTokens.colors.muscle.focused

        val bundles = fromAlpha(bundles, focusedColor)
        val colorMap = bundles.associate { it.first.muscle.type to it.second }
        return buildPreset(colorMap)
    }

    @Composable
    private fun generateByUniqueColor(
        bundles: ImmutableList<ExerciseExampleBundleState>
    ): MuscleColorPreset {
        val defaultColor = AppTokens.colors.muscle.inactive
        val palette = AppTokens.colors.muscle.palette

        val muscleTypes = remember(bundles) {
            bundles.map { it.muscle.type }.distinct()
        }

        val colorMap = muscleTypes.mapIndexed { index, muscle ->
            val color = palette.getOrElse(index) { palette.last() }
            muscle to color
        }.toMap()

        return buildPreset(colorMap, defaultColor)
    }

    @Composable
    private fun generateFromSelection(
        group: MuscleGroupState<*>,
        selectedIds: ImmutableSet<String>
    ): MuscleColorPreset {
        val muscles = remember(group.muscles) {
            group.muscles.map { it.value }.toImmutableSet()
        }
        val active = AppTokens.colors.muscle.active
        val inactive = AppTokens.colors.muscle.inactive

        val colorMap = muscles.associate {
            it.type to if (it.id in selectedIds) active else inactive
        }
        return buildPreset(colorMap, inactive)
    }

    @Composable
    private fun buildPreset(
        colorMap: Map<MuscleEnumState, Color>,
        fallback: Color = AppTokens.colors.muscle.inactive
    ): MuscleColorPreset {
        val get = { muscle: MuscleEnumState -> colorMap[muscle] ?: fallback }

        return MuscleColorPreset(
            biceps = get(MuscleEnumState.BICEPS),
            triceps = get(MuscleEnumState.TRICEPS),
            forearm = get(MuscleEnumState.FOREARM),
            forearmFront = get(MuscleEnumState.FOREARM),
            forearmBack = get(MuscleEnumState.FOREARM),
            lateralDeltoid = get(MuscleEnumState.LATERAL_DELTOID),
            anteriorDeltoid = get(MuscleEnumState.ANTERIOR_DELTOID),
            posteriorDeltoid = get(MuscleEnumState.POSTERIOR_DELTOID),
            pectoralisMajorAbdominal = get(MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL),
            pectoralisMajorClavicular = get(MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR),
            pectoralisMajorSternocostal = get(MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL),
            rectusAbdominis = get(MuscleEnumState.RECTUS_ABDOMINIS),
            obliquesAbdominis = get(MuscleEnumState.OBLIQUES),
            rhomboids = get(MuscleEnumState.RHOMBOIDS),
            latissimus = get(MuscleEnumState.LATISSIMUS_DORSI),
            trapezius = get(MuscleEnumState.TRAPEZIUS),
            teresMajor = get(MuscleEnumState.TERES_MAJOR),
            gluteal = get(MuscleEnumState.GLUTEAL),
            hamstrings = get(MuscleEnumState.HAMSTRINGS),
            calf = get(MuscleEnumState.CALF),
            quadriceps = get(MuscleEnumState.QUADRICEPS),
            adductors = get(MuscleEnumState.ADDUCTORS),
            abductors = get(MuscleEnumState.ABDUCTORS),
            other = fallback,
            outline = AppTokens.colors.muscle.outline,
            backgroundFront = AppTokens.colors.muscle.background,
            backgroundBack = AppTokens.colors.muscle.background
        )
    }

    @Composable
    private fun images(
        bundles: ImmutableList<ExerciseExampleBundleState>,
        preset: MuscleColorPreset
    ): Pair<ImageVector, ImageVector> {
        val frontImage = fullFront(preset)
        val backImage = fullBack(preset)
        return frontImage to backImage
    }

    @Composable
    private fun fromAlpha(
        bundles: ImmutableList<ExerciseExampleBundleState>,
        color: Color,
        minAlpha: Float = 0.3f,
        maxAlpha: Float = 1f
    ): List<Pair<ExerciseExampleBundleState, Color>> {
        if (bundles.isEmpty()) return emptyList()

        val sorted = remember(bundles) { bundles.sortedByDescending { it.percentage } }

        val alphas = remember(sorted) {
            if (sorted.size <= 1) listOf(1f)
            else {
                val step = (maxAlpha - minAlpha) / (sorted.size - 1)
                List(sorted.size) { i -> (maxAlpha - i * step).coerceIn(minAlpha, maxAlpha) }
            }
        }

        return sorted.mapIndexed { i, bundle ->
            bundle to color.copy(alpha = alphas[i])
        }
    }
}