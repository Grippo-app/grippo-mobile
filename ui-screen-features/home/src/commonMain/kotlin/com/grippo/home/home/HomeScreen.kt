package com.grippo.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.stubExerciseSpotlight
import com.grippo.core.state.metrics.stubMonthlyDigest
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.metrics.stubPerformanceMetrics
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.core.state.metrics.stubWeeklyDigest
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.DigestsCard
import com.grippo.design.components.metrics.HighlightsCard
import com.grippo.design.components.metrics.LastTrainingCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dashboard
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.start_workout
import com.grippo.home.home.components.EmptyHomeContent
import kotlinx.collections.immutable.ImmutableSet
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
    val hasHighlights = state.totalDuration != null &&
            state.streak != null

    val isEmptyState = state.lastTraining == null &&
            !hasHighlights &&
            state.weeklyDigest == null &&
            state.monthlyDigest == null

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

    if (isEmptyState && loaders.contains(HomeLoader.Trainings)) {
        Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        return@BaseComposeScreen
    }

    if (isEmptyState && loaders.contains(HomeLoader.Trainings).not()) {
        EmptyHomeContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onStartTraining = contract::onStartTraining
        )
        return@BaseComposeScreen
    }

    val basePadding = PaddingValues(
        horizontal = AppTokens.dp.screen.horizontalPadding,
        vertical = AppTokens.dp.contentPadding.content
    )

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.screen,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = resolvedPadding,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
            ) {
                if (state.lastTraining != null) {
                    item(key = "last_workout") {
                        LastTrainingCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.lastTraining,
                            onClick = contract::onOpenTrainings
                        )
                    }
                }

                if (hasHighlights) {
                    item(key = "highlight") {
                        HighlightsCard(
                            modifier = Modifier.fillMaxWidth(),
                            totalDuration = state.totalDuration,
                            spotlight = state.spotlight,
                            muscleLoad = state.muscleLoad,
                            streak = state.streak,
                            performance = state.performance,
                            onExampleClick = contract::onOpenExample,
                            onMuscleLoadingClick = contract::onOpenMuscleLoading,
                            onStreakClick = contract::onOpenTrainingStreak
                        )
                    }
                }

                if (state.monthlyDigest != null && state.weeklyDigest != null) {
                    item(key = "digest_section") {
                        DigestsCard(
                            weekly = state.weeklyDigest,
                            monthly = state.monthlyDigest,
                            onWeeklyClick = contract::onOpenWeeklyDigest,
                            onMonthlyClick = contract::onOpenMonthlyDigest
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_workout),
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onStartTraining
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
}

@AppPreview
@Composable
private fun HomeScreenPreview() {
    PreviewContainer {
        HomeScreen(
            state = HomeState(
                lastTraining = stubTraining(),
                weeklyDigest = stubWeeklyDigest(),
                monthlyDigest = stubMonthlyDigest(),
                totalDuration = 28.hours,
                spotlight = stubExerciseSpotlight(),
                muscleLoad = stubMuscleLoadSummary(),
                streak = stubTrainingStreaks().first(),
                performance = stubPerformanceMetrics(),
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenEmptyPreview() {
    PreviewContainer {
        HomeScreen(
            state = HomeState(),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}
