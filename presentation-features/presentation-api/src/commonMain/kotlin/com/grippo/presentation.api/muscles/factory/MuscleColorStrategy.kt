package com.grippo.presentation.api.muscles.factory

import com.grippo.presentation.api.exercise.example.models.ExerciseExampleBundleState
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

/**
 * Defines a strategy for generating muscle color presets.
 * This is used to dynamically color muscles based on different visual logic
 * such as selection or load intensity.
 */
public sealed interface MuscleColorStrategy {

    /**
     * A simple strategy that colors all visible muscles with the same color,
     * regardless of load or selection.
     */
    public data object Monochrome : MuscleColorStrategy

    /**
     * A strategy that colors muscles based on their involvement percentage
     * (e.g., in an exercise bundle), using the same base color but different alpha levels.
     *
     * The more involved the muscle is, the less transparent it appears.
     *
     * @param bundles A list of muscle bundles with their associated intensity percentages.
     */
    public data class ByAlpha(
        val bundles: ImmutableList<ExerciseExampleBundleState>,
    ) : MuscleColorStrategy

    /**
     * A strategy that assigns a unique color to each involved muscle.
     * This is useful for visual distinction when multiple muscles are involved,
     * without relying on alpha or selection.
     *
     * @param bundles A list of muscle bundles used to extract involved muscle types.
     */
    public data class ByUniqueColor(
        val bundles: ImmutableList<ExerciseExampleBundleState>
    ) : MuscleColorStrategy

    /**
     * A strategy that highlights only selected muscles within a group.
     * Selected muscles are shown with an active color, while the rest remain inactive.
     *
     * @param group The muscle group to select from.
     * @param selectedIds Set of muscle IDs to be marked as active.
     */
    public data class BySelection(
        val group: MuscleGroupState<*>,
        val selectedIds: ImmutableSet<String>
    ) : MuscleColorStrategy
}