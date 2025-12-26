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
import com.grippo.design.components.home.DigestsCard
import com.grippo.design.components.home.HighlightsCard
import com.grippo.design.components.home.LastTrainingCard
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
                            onViewWorkout = contract::onOpenTrainings
                        )
                    }
                }

                if (state.highlight != null) {
                    item(key = "highlight") {
                        HighlightsCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.highlight,
                            onViewWorkout = contract::onOpenTrainings,
                            onExampleClick = contract::onOpenExample
                        )
                    }
                }

                if (state.monthlyDigestState != null && state.weeklyDigestState != null) {
                    item(key = "digest_section") {
                        DigestsCard(
                            weekly = state.weeklyDigestState,
                            monthly = state.monthlyDigestState,
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
