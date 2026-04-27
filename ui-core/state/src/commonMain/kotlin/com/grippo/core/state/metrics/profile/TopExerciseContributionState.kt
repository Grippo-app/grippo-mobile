package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.CategoryEnumState

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
