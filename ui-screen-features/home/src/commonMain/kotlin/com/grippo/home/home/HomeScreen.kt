package com.grippo.home.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightGoodFrequency
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightNearBest
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightNeedsAttention
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightProgressWin
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.core.state.profile.stubUser
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.time.Duration.Companion.hours

@Composable
internal fun HomeScreen(
    state: HomeState,
    loaders: ImmutableSet<HomeLoader>,
    contract: HomeContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun HomeScreenPreview() {
    PreviewContainer {
        HomeScreen(
            state = HomeState(
                lastTraining = stubTraining(),
                totalDuration = 28.hours,
                spotlights = persistentListOf(
                    stubExerciseSpotlightNeedsAttention(),
                    stubExerciseSpotlightProgressWin(),
                    stubExerciseSpotlightGoodFrequency(),
                    stubExerciseSpotlightNearBest(),
                ),
                muscleLoad = stubMuscleLoadSummary(),
                streak = stubTrainingStreaks().random(),
                performance = stubPerformanceMetrics(),
                goalProgress = stubGoalProgressList().random(),
                hasDraftTraining = true,
                user = stubUser()
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenEmptyPreview() {
    val stub = remember { stubUser() }
    PreviewContainer {
        HomeScreen(
            state = HomeState(
                user = stub.copy(stats = stub.stats.copy(trainingsCount = 0)),
                excludedMusclesCount = 3,
                missingEquipmentCount = 5,
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}
