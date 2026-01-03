package com.grippo.toolkit.calculation

import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.toolkit.calculation.internal.muscle.MuscleImageBuilder
import com.grippo.toolkit.calculation.internal.strength.Estimated1RMAnalytics
import com.grippo.toolkit.calculation.models.MetricSeries
import com.grippo.toolkit.date.utils.DateRange

/**
 * Facade that exposes the most common workout analytics in a single place.
 * Every method intentionally mirrors how feature modules consume analytics today,
 * so callers do not need to know about the individual calculators underneath.
 */
public class AnalyticsApi(
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
) {
    private val estimated1RMAnalytics = Estimated1RMAnalytics(colorProvider, stringProvider)
    private val muscleImageBuilder = MuscleImageBuilder(colorProvider)

    /**
     * Produces an estimated 1RM series for the supplied [exercises].
     * Entries are already colourised and ready for chart rendering.
     */
    public suspend fun estimated1RmFromExercises(
        exercises: List<ExerciseState>
    ): MetricSeries = estimated1RMAnalytics
        .computeEstimated1RmFromExercises(exercises)

    /**
     * Produces an estimated 1RM series for [trainings] aggregated by the detected bucket scale.
     */
    public suspend fun estimated1RmFromTrainings(
        trainings: List<TrainingState>,
        range: DateRange,
    ): MetricSeries = estimated1RMAnalytics
        .computeEstimated1RmFromTrainings(trainings, range)

    /**
     * Builds a muscle color preset for a specific [group] with [selectedIds] highlighted.
     */
    public suspend fun musclePresetFromSelection(
        group: MuscleGroupState<MuscleRepresentationState.Plain>,
        selectedIds: Collection<String>,
    ): MuscleColorPreset = muscleImageBuilder
        .presetFromSelection(group, selectedIds.toSet())
}
