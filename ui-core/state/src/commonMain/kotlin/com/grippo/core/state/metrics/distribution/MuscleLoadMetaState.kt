package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.joined_pair
import com.grippo.design.resources.provider.metric_overview_muscle_sets
import com.grippo.design.resources.provider.metric_overview_muscle_workouts

@Immutable
public data class MuscleLoadMetaState(
    val trainingsCount: Int,
    val totalExercises: Int,
    val totalSets: Int,
    val totalRepetitions: Int,
    val totalVolume: Float,
    val dominantGroup: MuscleGroupEnumState?,
    val excludedSets: Int = 0,
    val unassignedSets: Int = 0,
    val unmeasuredVolumeSets: Int = 0,
) {
    @Composable
    public fun contextLine(): String {
        val workouts = AppTokens.strings.res(
            Res.plurals.metric_overview_muscle_workouts,
            trainingsCount,
            trainingsCount,
        )
        if (totalSets == 0) {
            return workouts
        }
        return AppTokens.strings.res(
            Res.string.joined_pair,
            workouts,
            AppTokens.strings.res(Res.plurals.metric_overview_muscle_sets, totalSets, totalSets),
        )
    }
}
