package com.grippo.toolkit.calculation.internal.muscle

import androidx.compose.ui.graphics.Color
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.muscles.fullBack
import com.grippo.design.resources.provider.muscles.fullFront
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.toolkit.calculation.models.MuscleColorSource
import com.grippo.toolkit.calculation.models.MuscleImages
import com.grippo.toolkit.calculation.models.MuscleLoadBreakdown

internal class MuscleImageBuilder(private val colorProvider: ColorProvider) {

    suspend fun generateImagesFromBreakdown(
        breakdown: MuscleLoadBreakdown,
    ): MuscleImages? {
        val entries = breakdown.entries
        if (entries.isEmpty()) return null

        return generateImagesFromSources(entries)
    }

    private suspend fun generateImagesFromSources(
        sources: List<MuscleColorSource>,
    ): MuscleImages? {
        if (sources.isEmpty()) return null

        val preset = buildPreset(sources)

        return MuscleImages(
            front = fullFront(preset),
            back = fullBack(preset),
        )
    }

    private suspend fun buildPreset(
        sources: List<MuscleColorSource>,
    ): MuscleColorPreset {
        val colors = colorProvider.get()

        val fallback = colors.muscle.inactive
        val outline = colors.muscle.outline
        val background = colors.muscle.background

        val colorMap: Map<MuscleEnumState, Color> = sources
            .flatMap { source ->
                source.muscles.map { muscle -> muscle to source.color }
            }
            .toMap()

        return buildPreset(colorMap, fallback, outline, background)
    }

    suspend fun presetFromSelection(
        group: MuscleGroupState<MuscleRepresentationState.Plain>,
        selectedIds: Set<String>,
    ): MuscleColorPreset {
        val colors = colorProvider.get()

        val fallback = colors.muscle.inactive
        val outline = colors.muscle.outline
        val background = colors.muscle.background
        val active = colors.muscle.active

        val colorMap = group.muscles.associate { muscleState ->
            val muscle = muscleState.value
            muscle.type to if (muscle.id in selectedIds) active else fallback
        }

        return buildPreset(colorMap, fallback, outline, background)
    }

    private fun buildPreset(
        colorMap: Map<MuscleEnumState, Color>,
        fallback: Color,
        outline: Color,
        background: Color,
    ): MuscleColorPreset {
        val resolve: (MuscleEnumState) -> Color =
            { muscle -> colorMap[muscle] ?: fallback }

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
            outline = outline,
            backgroundFront = background,
            backgroundBack = background,
        )
    }
}
