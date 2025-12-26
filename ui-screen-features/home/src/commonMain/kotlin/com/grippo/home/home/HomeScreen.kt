package com.grippo.home.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
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
import com.grippo.design.resources.provider.barbell
import com.grippo.design.resources.provider.box
import com.grippo.design.resources.provider.dumbbell
import com.grippo.design.resources.provider.home_empty_subtitle
import com.grippo.design.resources.provider.home_empty_title
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.plate
import com.grippo.design.resources.provider.start_workout
import com.grippo.design.resources.provider.weight
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
    val isEmptyState = state.lastTraining == null &&
            state.highlight == null &&
            state.weeklyDigestState == null &&
            state.monthlyDigestState == null

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
            if (isEmptyState) {
                EmptyHomeContent(
                    modifier = containerModifier,
                    contentPadding = resolvedPadding,
                    onStartTraining = contract::onStartTraining
                )
            } else {
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
            }
        },
        bottom = if (isEmptyState) {
            null
        } else {
            {
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
        }
    )
}

@Composable
private fun EmptyHomeContent(
    modifier: Modifier,
    contentPadding: PaddingValues,
    onStartTraining: () -> Unit
) {
    val title = AppTokens.strings.res(Res.string.home_empty_title)
    val subtitle = AppTokens.strings.res(Res.string.home_empty_subtitle)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        EmptyHomeDecorations(
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

            Text(
                text = subtitle,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_workout),
                ),
                style = ButtonStyle.Primary,
                onClick = onStartTraining
            )
        }
    }
}

@Composable
private fun EmptyHomeDecorations(modifier: Modifier = Modifier) {
    val largeIconSize = AppTokens.dp.home.empty.image

    val decorations = listOf(
        EmptyHomeIcon(
            painter = AppTokens.drawables.res(Res.drawable.dumbbell),
            alignment = Alignment.TopStart,
            offset = DpOffset(-(largeIconSize / 3), -(34.dp)),
            rotation = -12f,
            size = largeIconSize,
        ),
        EmptyHomeIcon(
            painter = AppTokens.drawables.res(Res.drawable.barbell),
            alignment = Alignment.TopEnd,
            offset = DpOffset((largeIconSize / 3), (52).dp),
            rotation = 0f,
            size = largeIconSize,
        ),
        EmptyHomeIcon(
            painter = AppTokens.drawables.res(Res.drawable.plate),
            alignment = Alignment.CenterStart,
            offset = DpOffset(-(largeIconSize / 2), -(34.dp)),
            rotation = -18f,
            size = largeIconSize,
        ),
        EmptyHomeIcon(
            painter = AppTokens.drawables.res(Res.drawable.box),
            alignment = Alignment.BottomEnd,
            offset = DpOffset((largeIconSize / 3), -(52).dp),
            rotation = -8f,
            size = largeIconSize,
        ),
        EmptyHomeIcon(
            painter = AppTokens.drawables.res(Res.drawable.weight),
            alignment = Alignment.BottomStart,
            offset = DpOffset(-(largeIconSize / 2), 60.dp),
            rotation = 10f,
            size = largeIconSize,
        ),
    )

    Box(modifier = modifier) {
        decorations.forEach { icon ->
            Image(
                modifier = Modifier
                    .align(icon.alignment)
                    .offset(x = icon.offset.x, y = icon.offset.y)
                    .size(icon.size)
                    .scale(1.3f)
                    .alpha(0.8f)
                    .graphicsLayer {
                        rotationZ = icon.rotation
                    },
                painter = icon.painter,
                contentDescription = null
            )
        }
    }
}

@Immutable
private data class EmptyHomeIcon(
    val painter: Painter,
    val alignment: Alignment,
    val offset: DpOffset,
    val rotation: Float,
    val size: Dp,
)

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
