package com.grippo.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.trainings.digest.stubMonthlyDigest
import com.grippo.core.state.trainings.digest.stubWeeklyDigest
import com.grippo.core.state.trainings.highlight.stubHighlight
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.home.HighlightsCard
import com.grippo.design.components.home.LastTrainingCard
import com.grippo.design.components.home.ThisMonthDigestCard
import com.grippo.design.components.home.ThisWeekDigestCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.start_workout
import kotlinx.collections.immutable.ImmutableSet
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
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
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

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.screen.verticalPadding
        ),
        overlay = AppTokens.colors.background.screen,
        content = { containerModifier, resolvedPadding ->
            LazyVerticalGrid(
                modifier = containerModifier
                    .fillMaxWidth()
                    .weight(1f),
                columns = GridCells.Fixed(2),
                contentPadding = resolvedPadding,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
            ) {
                state.lastTraining?.let { lastTraining ->
                    item(
                        key = "last_workout_card",
                        span = { GridItemSpan(2) }
                    ) {
                        LastTrainingCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = lastTraining,
                            onViewWorkout = contract::onOpenTrainings
                        )
                    }
                }

                state.highlight?.let { highlight ->
                    item(
                        key = "monthly_highlight",
                        span = { GridItemSpan(2) }
                    ) {
                        HighlightsCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = highlight,
                            onViewWorkout = contract::onOpenTrainings,
                            onExampleClick = contract::onOpenExample
                        )
                    }
                }

                state.weeklyDigestState?.let { weeklyDigest ->
                    item(
                        key = "weekly_digest",
                        span = { GridItemSpan(1) }
                    ) {
                        ThisWeekDigestCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = weeklyDigest,
                        )
                    }
                }

                state.monthlyDigestState?.let { monthlyDigest ->
                    item(
                        key = "monthly_digest",
                        span = { GridItemSpan(1) }
                    ) {
                        ThisMonthDigestCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = monthlyDigest,
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

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

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
                weeklyDigestState = stubWeeklyDigest(),
                monthlyDigestState = stubMonthlyDigest(),
                highlight = stubHighlight(),
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}
