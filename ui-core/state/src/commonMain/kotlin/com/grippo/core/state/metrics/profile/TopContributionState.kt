package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.muscles.MuscleEnumState

/**
 * UI-ready version of a single exercise's contribution to the period's
 * training stimulus. Shared by goal adherence (details screen) and the
 * training-profile summary.
 */
@Immutable
public data class TopExerciseContributionState(
    val exampleId: String,
    val name: String,
    val totalSets: Int,
    val stimulusShare: Int,          // % of period's total stimulus (0..100)
    val heaviestWeight: Float,
    val estimatedOneRepMax: Float,
    val category: CategoryEnumState?,
)

/**
 * UI-ready version of a single muscle's contribution to the period's
 * training stimulus. Shared by goal adherence (details screen) and the
 * training-profile summary.
 */
@Immutable
public data class TopMuscleContributionState(
    val muscle: MuscleEnumState,
    val share: Int,                  // % of period's total muscle work (0..100)
)
