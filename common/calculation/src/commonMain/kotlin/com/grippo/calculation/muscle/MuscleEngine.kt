package com.grippo.calculation.muscle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.calculation.muscle.factory.MuscleColorSource
import com.grippo.calculation.muscle.factory.MuscleColorStrategy
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.muscles.fullBack
import com.grippo.design.resources.provider.muscles.fullFront
import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlin.math.roundToInt

public object MuscleEngine {

    @Composable
    public fun generatePreset(strategy: MuscleColorStrategy): MuscleColorPreset = when (strategy) {
        is MuscleColorStrategy.BySelection -> generateFromSelection(strategy.group, strategy.selectedIds)
        is MuscleColorStrategy.ByScaleStops -> generateByScaleStops(strategy.bundles)
        is MuscleColorStrategy.BySources -> generateFromSources(strategy.sources)
    }

    @Composable
    public fun generatePreset(sources: List<MuscleColorSource>): MuscleColorPreset =
        generatePreset(MuscleColorStrategy.BySources(sources))

    @Composable
    public fun generateImages(preset: MuscleColorPreset): Pair<ImageVector, ImageVector> =
        fullFront(preset) to fullBack(preset)

    @Composable
    public fun generateImages(
        preset: MuscleColorPreset,
        bundles: ImmutableList<ExerciseExampleBundleState>,
    ): Pair<ImageVector, ImageVector> = generateImages(preset)

    @Composable
    private fun generateByScaleStops(
        bundles: ImmutableList<ExerciseExampleBundleState>,
    ): MuscleColorPreset {
        val colors = AppTokens.colors.palette.palette5OrangeRedGrowth
        val ranked = fromStopsRanked(bundles, colors)
        val colorMap = ranked.associate { it.first.muscle.type to it.second }
        return buildPreset(colorMap)
    }

    @Composable
    private fun generateFromSelection(
        group: MuscleGroupState<*>,
        selectedIds: ImmutableSet<String>,
    ): MuscleColorPreset {
        val muscles = remember(group.muscles) {
            group.muscles.map { it.value }.toImmutableSet()
        }
        val active = AppTokens.colors.muscle.active
        val inactive = AppTokens.colors.muscle.inactive

        val colorMap = muscles.associate { muscleState ->
            muscleState.type to if (muscleState.id in selectedIds) active else inactive
        }
        return buildPreset(colorMap, inactive)
    }

    @Composable
    private fun generateFromSources(sources: List<MuscleColorSource>): MuscleColorPreset {
        if (sources.isEmpty()) return buildPreset(emptyMap())

        val colorMap = sources.flatMap { source ->
            source.muscles.map { muscle -> muscle to source.color }
        }.toMap()

        return buildPreset(colorMap)
    }

    @Composable
    private fun fromStopsRanked(
        bundles: ImmutableList<ExerciseExampleBundleState>,
        paletteAscending: List<Color>,
    ): List<Pair<ExerciseExampleBundleState, Color>> {
        if (bundles.isEmpty() || paletteAscending.isEmpty()) return emptyList()
        val sorted = remember(bundles) { bundles.sortedByDescending { it.percentage.value } }
        val n = sorted.size
        val last = paletteAscending.lastIndex
        return sorted.mapIndexed { index, bundle ->
            val pos = if (n == 1) 1f else 1f - (index.toFloat() / (n - 1))
            val paletteIndex = (pos * last).roundToInt().coerceIn(0, last)
            bundle to paletteAscending[paletteIndex]
        }
    }

    @Composable
    private fun buildPreset(
        colorMap: Map<MuscleEnumState, Color>,
        fallback: Color = AppTokens.colors.muscle.inactive,
    ): MuscleColorPreset {
        val resolve = { muscle: MuscleEnumState -> colorMap[muscle] ?: fallback }

        return MuscleColorPreset(
            biceps = resolve(MuscleEnumState.BICEPS),
            triceps = resolve(MuscleEnumState.TRICEPS),
            forearm = resolve(MuscleEnumState.FOREARM),
            forearmFront = resolve(MuscleEnumState.FOREARM),
            forearmBack = resolve(MuscleEnumState.FOREARM),
            lateralDeltoid = resolve(MuscleEnumState.LATERAL_DELTOID),
            anteriorDeltoid = resolve(MuscleEnumState.ANTERIOR_DELTOID),
            posteriorDeltoid = resolve(MuscleEnumState.POSTERIOR_DELTOID),
            pectoralisMajorAbdominal = resolve(MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL),
            pectoralisMajorClavicular = resolve(MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR),
            pectoralisMajorSternocostal = resolve(MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL),
            rectusAbdominis = resolve(MuscleEnumState.RECTUS_ABDOMINIS),
            obliquesAbdominis = resolve(MuscleEnumState.OBLIQUES),
            rhomboids = resolve(MuscleEnumState.RHOMBOIDS),
            latissimus = resolve(MuscleEnumState.LATISSIMUS_DORSI),
            trapezius = resolve(MuscleEnumState.TRAPEZIUS),
            teresMajor = resolve(MuscleEnumState.TERES_MAJOR),
            gluteal = resolve(MuscleEnumState.GLUTEAL),
            hamstrings = resolve(MuscleEnumState.HAMSTRINGS),
            calf = resolve(MuscleEnumState.CALF),
            quadriceps = resolve(MuscleEnumState.QUADRICEPS),
            adductors = resolve(MuscleEnumState.ADDUCTORS),
            abductors = resolve(MuscleEnumState.ABDUCTORS),
            other = fallback,
            outline = AppTokens.colors.muscle.outline,
            backgroundFront = AppTokens.colors.muscle.background,
            backgroundBack = AppTokens.colors.muscle.background,
        )
    }
}
