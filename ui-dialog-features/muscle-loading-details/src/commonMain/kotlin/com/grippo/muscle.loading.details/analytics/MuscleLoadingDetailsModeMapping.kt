package com.grippo.muscle.loading.details.analytics

import com.grippo.analytics.muscle.loading.details.models.MuscleLoadingDetailsMode
import com.grippo.muscle.loading.details.MuscleLoadingDetailsShowingMode

internal fun MuscleLoadingDetailsShowingMode.toAnalytics(): MuscleLoadingDetailsMode = when (this) {
    MuscleLoadingDetailsShowingMode.PerGroup -> MuscleLoadingDetailsMode.PerGroup
    MuscleLoadingDetailsShowingMode.PerMuscle -> MuscleLoadingDetailsMode.PerMuscle
}
