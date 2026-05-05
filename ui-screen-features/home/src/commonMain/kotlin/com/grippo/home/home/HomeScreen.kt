package com.grippo.home.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.grippo.core.state.metrics.profile.stubTrainingLoadProfile
import com.grippo.core.state.profile.stubUser
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dashboard
import com.grippo.design.resources.provider.icons.User
import com.grippo.home.home.components.DashboardHomeContent
import com.grippo.home.home.components.WelcomeHomeContent
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.time.Duration.Companion.hours

@Composable
internal fun HomeScreen(
    state: HomeState,
    loaders: ImmutableSet<HomeLoader>,
    contract: HomeContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    )
) {
    val isEmptyState = (state.user?.stats?.trainingsCount ?: 0) < 1
    val isLoading = (loaders.contains(HomeLoader.Trainings) || state.user == null) && isEmptyState

    if (isLoading) {
        Loader(
            modifier = Modifier.fillMaxSize()
        )
        return@BaseComposeScreen
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
        title = AppTokens.strings.res(Res.string.dashboard),
        trailing = {
            Button(
                modifier = Modifier.padding(end = AppTokens.dp.contentPadding.subContent),
                content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.User)),
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                onClick = contract::onOpenProfile
            )
        },
    )

    if (isEmptyState && state.user != null) {
        WelcomeHomeContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            user = state.user,
            experience = state.user.experience,
            excludedMusclesCount = state.excludedMusclesCount,
            missingEquipmentCount = state.missingEquipmentCount,
            hasDraftTraining = state.hasDraftTraining,
            onStartTraining = contract::onStartTraining,
            onResumeTraining = contract::onResumeTraining,
        )
        return@BaseComposeScreen
    }

    DashboardHomeContent(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        state = state,
        contract = contract,
    )
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
                profile = stubTrainingLoadProfile(),
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
