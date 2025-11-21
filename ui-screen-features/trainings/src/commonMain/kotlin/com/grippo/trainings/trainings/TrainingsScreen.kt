package com.grippo.trainings.trainings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Gym
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.start_workout
import com.grippo.design.resources.provider.trainings
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.trainings.trainings.components.DailyTrainingsPage
import com.grippo.trainings.trainings.components.MonthlyTrainingsPage
import com.grippo.trainings.trainings.components.WeeklyTrainingsPage
import com.grippo.trainings.trainings.utilities.TrainingsPagerOffsets
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
internal fun TrainingsScreen(
    state: TrainingsState,
    loaders: ImmutableSet<TrainingsLoader>,
    contract: TrainingsContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
        ambient = ScreenBackground.Ambient(
            color = AppTokens.colors.brand.color5,
        )
    )
) {
    val allowedOffsets = TrainingsPagerOffsets

    val centerPageIndex = allowedOffsets.indexOf(0).coerceAtLeast(0)

    val pagerState = rememberPagerState(
        initialPage = centerPageIndex,
        pageCount = { allowedOffsets.size }
    )

    val periodSegmentItems = remember {
        TrainingsTimelinePeriod.entries.map { it to it.text }.toPersistentList()
    }

    LaunchedEffect(centerPageIndex, allowedOffsets) {
        snapshotFlow { pagerState.isScrollInProgress to pagerState.currentPage }
            .filter { (inProgress, _) -> !inProgress }
            .map { (_, page) -> page }
            .distinctUntilChanged()
            .collectLatest { page ->
                if (page != centerPageIndex) {
                    val delta = allowedOffsets.getOrNull(page) ?: 0
                    contract.onShiftDate(delta)
                    pagerState.scrollToPage(centerPageIndex)
                }
            }
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
        style = ToolbarStyle.Transparent,
        trailing = {
            Button(
                modifier = Modifier.padding(end = AppTokens.dp.contentPadding.subContent),
                content = ButtonContent.Icon(icon = AppTokens.icons.User),
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                onClick = contract::onOpenProfile
            )
        },
        content = {
            Segment(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                items = periodSegmentItems,
                selected = state.period,
                onSelect = contract::onSelectPeriod,
                segmentWidth = SegmentWidth.EqualFill,
                style = SegmentStyle.Outline
            )
        }
    )

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
            HorizontalPager(
                modifier = containerModifier
                    .fillMaxWidth()
                    .weight(1f),
                state = pagerState
            ) { page ->
                val pageOffset = allowedOffsets.getOrNull(page) ?: 0
                val pageTrainings = remember(state.trainings, pageOffset) {
                    state.trainings[pageOffset] ?: persistentListOf()
                }

                key(state.period, state.date, pageOffset) {
                    when (state.period) {
                        TrainingsTimelinePeriod.Daily -> DailyTrainingsPage(
                            trainings = pageTrainings,
                            contentPadding = resolvedPadding,
                            onViewStatsClick = contract::onDailyDigestViewStats,
                            onTrainingMenuClick = contract::onTrainingMenuClick,
                            onExerciseClick = contract::onExerciseClick,
                        )

                        TrainingsTimelinePeriod.Weekly -> WeeklyTrainingsPage(
                            trainings = pageTrainings,
                            contentPadding = resolvedPadding,
                            onViewStatsClick = contract::onDailyDigestViewStats,
                            onOpenDaily = contract::onOpenDaily,
                        )

                        TrainingsTimelinePeriod.Monthly -> MonthlyTrainingsPage(
                            trainings = pageTrainings,
                            contentPadding = resolvedPadding,
                            onViewStatsClick = contract::onDailyDigestViewStats,
                            onOpenDaily = contract::onOpenDaily,
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(1f),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_workout),
                    startIcon = AppTokens.icons.Gym
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onAddTraining
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
}

@AppPreview
@Composable
private fun DailyScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = TrainingsState(
                period = TrainingsTimelinePeriod.Daily,
                date = TrainingsTimelinePeriod.Daily.defaultRange(),
                trainings = persistentMapOf(
                    0 to persistentListOf(
                        stubTraining(),
                        stubTraining(),
                    ).transformToTrainingListValue(
                        range = TrainingsTimelinePeriod.Daily.defaultRange()
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun WeeklyScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = TrainingsState(
                period = TrainingsTimelinePeriod.Weekly,
                date = TrainingsTimelinePeriod.Weekly.defaultRange(),
                trainings = persistentMapOf(
                    0 to persistentListOf(
                        stubTraining(),
                        stubTraining(),
                    ).transformToTrainingListValue(
                        range = TrainingsTimelinePeriod.Weekly.defaultRange()
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun MonthlyScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = TrainingsState(
                period = TrainingsTimelinePeriod.Monthly,
                date = TrainingsTimelinePeriod.Monthly.defaultRange(),
                trainings = persistentMapOf(
                    0 to persistentListOf(
                        stubTraining(),
                        stubTraining(),
                    ).transformToTrainingListValue(
                        range = TrainingsTimelinePeriod.Monthly.defaultRange()
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty
        )
    }
}
