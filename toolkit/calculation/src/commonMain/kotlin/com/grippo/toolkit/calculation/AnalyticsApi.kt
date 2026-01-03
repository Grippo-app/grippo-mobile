package com.grippo.toolkit.calculation

import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.toolkit.calculation.internal.muscle.MuscleImageBuilder

/**
 * Facade that exposes the most common workout analytics in a single place.
 * Every method intentionally mirrors how feature modules consume analytics today,
 * so callers do not need to know about the individual calculators underneath.
 */
public class AnalyticsApi(
    colorProvider: ColorProvider,
) {
    private val muscleImageBuilder = MuscleImageBuilder(colorProvider)

    /**
     * Builds a muscle color preset for a specific [group] with [selectedIds] highlighted.
     */
    public suspend fun musclePresetFromSelection(
        group: MuscleGroupState<MuscleRepresentationState.Plain>,
        selectedIds: Collection<String>,
    ): MuscleColorPreset = muscleImageBuilder
        .presetFromSelection(group, selectedIds.toSet())
}
