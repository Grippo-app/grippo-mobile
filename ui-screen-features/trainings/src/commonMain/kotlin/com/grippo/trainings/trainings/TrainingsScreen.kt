package com.grippo.trainings.trainings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.trainings.stubDailyTrainingTimeline
import com.grippo.core.state.trainings.stubMonthlyTrainingTimeline
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.datetime.DatePicker
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.start_workout
import com.grippo.design.resources.provider.trainings
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.trainings.trainings.components.DailyTrainingsPage
import com.grippo.trainings.trainings.components.MonthlyTrainingsPage
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun TrainingsScreen(
    state: TrainingsState,
    loaders: ImmutableSet<TrainingsLoader>,
    contract: TrainingsContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
    )
) {
    val periodSegmentItems = remember {
        TrainingsTimelinePeriod.entries.map { it to it.text }.toPersistentList()
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
        leading = Leading.Back(contract::onBack),
        style = ToolbarStyle.Transparent,
        content = {
            Segment(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                items = periodSegmentItems,
                selected = state.period,
                onSelect = contract::onSelectPeriod,
                segmentWidth = SegmentWidth.EqualFill,
                style = SegmentStyle.Fill
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            DatePicker(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                value = state.date.from,
                format = when (state.period) {
                    TrainingsTimelinePeriod.Daily -> DateFormat.DateOnly.DateDdMmm
                    TrainingsTimelinePeriod.Monthly -> DateFormat.DateOnly.Mmmm
                },
                limitations = state.limitations,
                onSelect = contract::onOpenDateSelector,
                onNext = contract::onSelectNextDate,
                onPrevious = contract::onSelectPreviousDate
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
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
            when (state.period) {
                TrainingsTimelinePeriod.Daily -> DailyTrainingsPage(
                    modifier = containerModifier
                        .fillMaxWidth()
                        .weight(1f),
                    timeline = state.timeline,
                    contentPadding = resolvedPadding,
                    onViewStatsClick = contract::onDailyDigestViewStats,
                    onTrainingMenuClick = contract::onTrainingMenuClick,
                    onExerciseClick = contract::onExerciseClick,
                )

                TrainingsTimelinePeriod.Monthly -> MonthlyTrainingsPage(
                    modifier = containerModifier
                        .fillMaxWidth()
                        .weight(1f),
                    timeline = state.timeline,
                    contentPadding = resolvedPadding,
                    month = state.date.from.date,
                    onDigestClick = contract::onDailyDigestViewStats,
                    onOpenDaily = contract::onOpenDaily,
                )
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
                timeline = stubDailyTrainingTimeline(),
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
                timeline = stubMonthlyTrainingTimeline(),
            ),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty
        )
    }
}
