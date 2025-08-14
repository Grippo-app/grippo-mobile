package com.grippo.state.muscles.factory

import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import com.grippo.state.muscles.MuscleGroupState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

/**
 * Strategies for coloring muscles in visualizations.
 *
 * These strategies don't hard-code specific colors: concrete shades come from your theme
 * (e.g., AppTokens.colors.muscle: focused/active/inactive/text/palette/scaleStops).
 */
public sealed interface MuscleColorStrategy {

    /**
     * Monochrome coloring: all involved muscles get the same color,
     * regardless of intensity or selection.
     *
     * Useful for simple states like “everything active”.
     */
    public data object Monochrome : MuscleColorStrategy

    /**
     * Alpha-based grading relative to a single base color.
     *
     * The more involved a muscle is, the less transparent (more opaque) it appears;
     * less involved muscles are rendered with higher transparency.
     *
     * Use when you want to keep one hue and only hint at the hierarchy.
     *
     * @param bundles A list of muscle bundles with involvement percentages; ordering is
     * used to distribute alpha values.
     */
    public data class ByAlpha(
        val bundles: ImmutableList<ExerciseExampleBundleState>,
    ) : MuscleColorStrategy

    /**
     * Color-scale grading (scaleStops) without changing alpha.
     *
     * Muscles are ranked by involvement and mapped onto the color scale provided by
     * AppTokens.colors.muscle.scaleStops (e.g., cold → warm), producing clear contrast
     * in both light and dark themes.
     *
     * Use when precise visual differentiation of intensity matters.
     *
     * @param bundles A list of muscle bundles with involvement percentages; used for
     * ranking along the scale.
     */
    public data class ByScaleStops(
        val bundles: ImmutableList<ExerciseExampleBundleState>,
    ) : MuscleColorStrategy

    /**
     * Assigns a unique color per involved muscle, ignoring the magnitude of involvement.
     *
     * Helpful when the primary goal is to distinguish muscles from each other rather than
     * visualize relative intensity. Colors are taken from a palette (e.g., muscle.palette
     * or a global categorical palette).
     *
     * @param bundles A list of muscle bundles; values are only used to extract muscle types.
     */
    public data class ByUniqueColor(
        val bundles: ImmutableList<ExerciseExampleBundleState>
    ) : MuscleColorStrategy

    /**
     * Highlights only selected muscles within a group.
     *
     * Selected muscles use the theme’s “active” color; the rest remain “inactive”.
     * Ideal for selection/filtering modes.
     *
     * @param group The muscle group to select from.
     * @param selectedIds Set of muscle IDs to be marked as active.
     */
    public data class BySelection(
        val group: MuscleGroupState<*>,
        val selectedIds: ImmutableSet<String>
    ) : MuscleColorStrategy
}