package com.grippo.core.state.achevements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.best_tonnage
import com.grippo.design.resources.provider.best_weight
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.lifetime_volume
import com.grippo.design.resources.provider.max_repetitions
import com.grippo.design.resources.provider.peak_intensity
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public sealed class AchievementState(
    public val key: String,
    public open val exerciseExampleId: String
) {
    public data class BestTonnage(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val tonnage: Int
    ) : AchievementState(
        key = "best_tonnage",
        exerciseExampleId = exerciseExampleId,
    )

    public data class BestWeight(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val iterationId: String,
        val weight: Int
    ) : AchievementState(
        key = "best_weight",
        exerciseExampleId = exerciseExampleId,
    )

    public data class LifetimeVolume(
        override val exerciseExampleId: String,
        val totalVolume: Int,
        val sessionsCount: Int
    ) : AchievementState(
        key = "lifetime_volume",
        exerciseExampleId = exerciseExampleId,
    )

    public data class MaxRepetitions(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val iterationId: String,
        val totalVolume: Int,
        val repetitions: Int
    ) : AchievementState(
        key = "max_repetitions",
        exerciseExampleId = exerciseExampleId,
    )

    public data class PeakIntensity(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val intensity: Double,
    ) : AchievementState(
        key = "peak_intensity",
        exerciseExampleId = exerciseExampleId,
    )

    public fun text(): UiText {
        return when (this) {
            is BestTonnage -> UiText.Res(Res.string.best_tonnage)
            is BestWeight -> UiText.Res(Res.string.best_weight)
            is LifetimeVolume -> UiText.Res(Res.string.lifetime_volume)
            is MaxRepetitions -> UiText.Res(Res.string.max_repetitions)
            is PeakIntensity -> UiText.Res(Res.string.peak_intensity)
        }
    }

    @Composable
    public fun value(): ImageVector {
        return AppTokens.icons.Calendar // todo remove it (use value instead of)
    }
}

public fun stubAchievements(): PersistentList<AchievementState> = persistentListOf(
    stubBestTonnageAchievement(),
    stubBestWeightAchievement(),
    stubLifetimeVolumeAchievement(),
    stubMaxRepetitionsAchievement(),
    stubPeakIntensityAchievement(),
)

public fun stubAchievement(): AchievementState {
    return when (Random.nextInt(0, 5)) {
        0 -> stubBestTonnageAchievement()
        1 -> stubBestWeightAchievement()
        2 -> stubLifetimeVolumeAchievement()
        3 -> stubMaxRepetitionsAchievement()
        else -> stubPeakIntensityAchievement()
    }
}

private fun stubBestTonnageAchievement(): AchievementState.BestTonnage {
    return AchievementState.BestTonnage(
        exerciseExampleId = Uuid.random().toString(),
        exerciseId = Uuid.random().toString(),
        tonnage = Random.nextInt(1_000, 12_000)
    )
}

private fun stubBestWeightAchievement(): AchievementState.BestWeight {
    return AchievementState.BestWeight(
        exerciseExampleId = Uuid.random().toString(),
        exerciseId = Uuid.random().toString(),
        iterationId = Uuid.random().toString(),
        weight = Random.nextInt(20, 280)
    )
}

private fun stubLifetimeVolumeAchievement(): AchievementState.LifetimeVolume {
    return AchievementState.LifetimeVolume(
        exerciseExampleId = Uuid.random().toString(),
        totalVolume = Random.nextInt(5_000, 50_000),
        sessionsCount = Random.nextInt(10, 120)
    )
}

private fun stubMaxRepetitionsAchievement(): AchievementState.MaxRepetitions {
    return AchievementState.MaxRepetitions(
        exerciseExampleId = Uuid.random().toString(),
        exerciseId = Uuid.random().toString(),
        iterationId = Uuid.random().toString(),
        totalVolume = Random.nextInt(500, 10_000),
        repetitions = Random.nextInt(3, 25)
    )
}

private fun stubPeakIntensityAchievement(): AchievementState.PeakIntensity {
    return AchievementState.PeakIntensity(
        exerciseExampleId = Uuid.random().toString(),
        exerciseId = Uuid.random().toString(),
        intensity = Random.nextDouble(0.75, 2.5)
    )
}