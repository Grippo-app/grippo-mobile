package com.grippo.home.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetrics
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dashboard
import com.grippo.home.home.components.DashboardHomeContent
import com.grippo.home.home.components.HomeLoadingContent
import com.grippo.home.home.components.WelcomeHomeContent
import com.grippo.home.home.components.homePreviewState
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

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
    val hasPeriodContent = state.lastTraining != null || state.muscleLoad != null ||
        state.streak != null || state.goalProgress != null || state.spotlights.isNotEmpty() ||
        state.performance.any { !it.isEmpty() }
    // A completed empty period still publishes five Empty-status performance metrics.
    val awaitsInitialMetrics = state.performance.isEmpty()
    val isLoading = state.range.value == null || state.user == null ||
        awaitsInitialMetrics ||
        (loaders.contains(HomeLoader.Trainings) && (isEmptyState || !hasPeriodContent))

    if (isLoading) {
        Toolbar(
            modifier = Modifier.fillMaxWidth(),
            style = ToolbarStyle.Transparent,
            title = AppTokens.strings.res(Res.string.dashboard),
        )
        HomeLoadingContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = state,
            onPeriodChange = contract::onOpenPeriodPicker,
        )
        return@BaseComposeScreen
    }

    if (isEmptyState) {
        WelcomeHomeContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .statusBarsPadding(),
            user = state.user,
            experience = state.user.experience,
            excludedMusclesCount = state.excludedMusclesCount,
            missingEquipmentCount = state.missingEquipmentCount,
            hasGoal = state.hasGoal,
            showWelcomeConfetti = state.showWelcomeConfetti,
            onWelcomeConfettiShown = contract::onWelcomeConfettiShown,
        )
        return@BaseComposeScreen
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
        title = AppTokens.strings.res(Res.string.dashboard),
    )

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
private fun HomeScreenPopulatedPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState(),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenWelcomeWithGoalPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState(lifetimeTrainingCount = 0),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenWelcomeWithoutGoalPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState(lifetimeTrainingCount = 0, hasGoal = false),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenFirstTrainingLoadingPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState(lifetimeTrainingCount = 1, hasGoal = false).copy(
                lastTraining = null,
                muscleLoad = null,
                streak = null,
                spotlights = persistentListOf(),
                performance = persistentListOf(),
            ),
            loaders = persistentSetOf(HomeLoader.Trainings),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenReturningUserLoadingPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState().copy(
                lastTraining = null,
                muscleLoad = null,
                streak = null,
                goalProgress = null,
                spotlights = persistentListOf(),
                performance = persistentListOf(),
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenUserLoadingPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState().copy(user = null),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenEmptyCacheRefreshingPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState().copy(
                lastTraining = null,
                muscleLoad = null,
                streak = null,
                goalProgress = null,
                spotlights = persistentListOf(),
                performance = stubEmptyPerformanceMetrics(),
            ),
            loaders = persistentSetOf(HomeLoader.Trainings),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenCachedRefreshingPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState(),
            loaders = persistentSetOf(HomeLoader.Trainings),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenDurationUnlockedPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState(lifetimeTrainingCount = 2, hasGoal = false).copy(
                spotlights = persistentListOf(),
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenAllMetricsUnlockedPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState(lifetimeTrainingCount = 3).copy(
                spotlights = persistentListOf(),
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenGoalOnlyBannerPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState(lifetimeTrainingCount = 3, hasGoal = false).copy(
                lastTraining = null,
                muscleLoad = null,
                streak = null,
                spotlights = persistentListOf(),
                performance = stubEmptyPerformanceMetrics(),
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenEmptyPeriodPreview() {
    PreviewContainer {
        HomeScreen(
            state = homePreviewState().copy(
                lastTraining = null,
                muscleLoad = null,
                streak = null,
                goalProgress = null,
                spotlights = persistentListOf(),
                performance = stubEmptyPerformanceMetrics(),
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenLoadingPreview() {
    PreviewContainer {
        HomeScreen(
            state = HomeState(
                range = DateRangeFormatState.Empty(kind = DateRangeKind.Last30Days),
            ),
            loaders = persistentSetOf(HomeLoader.Trainings),
            contract = HomeContract.Empty,
        )
    }
}
