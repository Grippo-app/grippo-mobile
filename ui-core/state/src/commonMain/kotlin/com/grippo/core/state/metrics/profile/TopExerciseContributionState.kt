package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.training_profile_artifact_sets
import com.grippo.design.resources.provider.training_profile_artifact_top_exercise_meta_e1rm
import com.grippo.design.resources.provider.training_profile_artifact_top_exercise_meta_full
import com.grippo.design.resources.provider.training_profile_artifact_top_exercise_meta_heaviest
import kotlin.math.roundToInt

@Immutable
public data class TopExerciseContributionState(
    val exampleId: String,
    val name: String,
    val totalSets: Int,
    val stimulusShare: Int,          // % of period's total stimulus (0..100)
    val heaviestWeight: Float,
    val estimatedOneRepMax: Float,
    val category: CategoryEnumState?,
) {
    @Composable
    public fun caption(): String {
        val kgSuffix = AppTokens.strings.res(Res.string.kg)

        val setsCaption = AppTokens.strings.res(
            Res.string.training_profile_artifact_sets,
            totalSets,
        )

        val heaviest = heaviestWeight.takeIf { it > 0f }
        val e1rm = estimatedOneRepMax.takeIf { it > 0f }

        val meta = when {
            heaviest != null && e1rm != null -> AppTokens.strings.res(
                Res.string.training_profile_artifact_top_exercise_meta_full,
                "${heaviest.roundToInt()} $kgSuffix",
                "${e1rm.roundToInt()} $kgSuffix",
            )

            heaviest != null -> AppTokens.strings.res(
                Res.string.training_profile_artifact_top_exercise_meta_heaviest,
                "${heaviest.roundToInt()} $kgSuffix",
            )

            e1rm != null -> AppTokens.strings.res(
                Res.string.training_profile_artifact_top_exercise_meta_e1rm,
                "${e1rm.roundToInt()} $kgSuffix",
            )

            else -> null
        }

        return if (meta != null) "$setsCaption · $meta" else setsCaption
    }
}
