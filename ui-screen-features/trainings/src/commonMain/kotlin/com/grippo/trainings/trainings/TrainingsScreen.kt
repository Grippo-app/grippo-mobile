package com.grippo.trainings.trainings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.stubDailyTrainingTimeline
import com.grippo.core.state.trainings.stubMonthlyTrainingTimeline
import com.grippo.design.components.datetime.DatePicker
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.trainings
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.trainings.trainings.components.DailyTrainingsPage
import com.grippo.trainings.trainings.components.DailyTrainingsSkeleton
import com.grippo.trainings.trainings.components.MonthlyTrainingsPage
import com.grippo.trainings.trainings.components.MonthlyTrainingsSkeleton
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Composable
internal fun TrainingsScreen(
    state: TrainingsState,
    loaders: ImmutableSet<TrainingsLoader>,
    contract: TrainingsContract,
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    ),
) {
    val periodSegmentItems = remember {
        TrainingsTimelinePeriod.Variants
            .map { it.id to it.text }
            .toPersistentList()
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
        style = ToolbarStyle.Transparent,
        content = {
            Segment(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                items = periodSegmentItems,
                selected = state.period.id,
                onSelect = contract::onSelectPeriod,
                segmentWidth = SegmentWidth.EqualFill,
                style = SegmentStyle.Fill,
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            DatePicker(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                value = state.date.from,
                format = when (state.period) {
                    is TrainingsTimelinePeriod.Daily -> DateFormat.DateOnly.DateDdMmm
                    is TrainingsTimelinePeriod.Monthly -> DateFormat.DateOnly.MonthFullStandalone
                },
                contextual = state.period is TrainingsTimelinePeriod.Daily,
                onSelect = contract::onOpenDateSelector,
                onNext = contract::onSelectNextDate,
                onPrevious = contract::onSelectPreviousDate,
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            ContentDivider()
        }
    )

    val contentPadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content,
        end = AppTokens.dp.screen.horizontalPadding,
        bottom = AppTokens.dp.screen.verticalPadding,
    )

    val contentModifier = Modifier.fillMaxWidth().weight(1f).navigationBarsPadding()
    val isLoading = TrainingsLoader.Trainings in loaders

    when (val period = state.period) {
        is TrainingsTimelinePeriod.Daily -> {
            if (isLoading && period.items.isEmpty()) {
                DailyTrainingsSkeleton(
                    modifier = contentModifier,
                    contentPadding = contentPadding
                )
            } else {
                DailyTrainingsPage(
                    modifier = contentModifier,
                    items = period.items,
                    contentPadding = contentPadding,
                    onTrainingMenuClick = contract::onTrainingMenuClick,
                    onExerciseClick = contract::onExerciseClick,
                )
            }
        }

        is TrainingsTimelinePeriod.Monthly -> {
            if (isLoading && period.digest is DigestState.Empty) {
                MonthlyTrainingsSkeleton(
                    modifier = contentModifier,
                    contentPadding = contentPadding,
                    weekCount = period.weeks.size.coerceAtLeast(4),
                )
            } else {
                MonthlyTrainingsPage(
                    modifier = contentModifier,
                    period = period,
                    contentPadding = contentPadding,
                    onDigestClick = contract::onDailyDigestViewStats,
                    onOpenDaily = contract::onOpenDaily,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun DailyScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = dailyTrainingsPreviewState(),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun MonthlyScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = monthlyTrainingsPreviewState(),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DailyEmptyScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = dailyTrainingsPreviewState(empty = true),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun DailyLoadingScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = dailyTrainingsPreviewState(empty = true),
            loaders = persistentSetOf(TrainingsLoader.Trainings),
            contract = TrainingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun MonthlyEmptyScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = monthlyTrainingsPreviewState(empty = true),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun MonthlyLoadingScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = monthlyTrainingsPreviewState(empty = true),
            loaders = persistentSetOf(TrainingsLoader.Trainings),
            contract = TrainingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun MonthlyRefreshingScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = monthlyTrainingsPreviewState(),
            loaders = persistentSetOf(TrainingsLoader.Trainings),
            contract = TrainingsContract.Empty,
        )
    }
}

private fun dailyTrainingsPreviewState(empty: Boolean = false): TrainingsState {
    val items = if (empty) {
        persistentListOf()
    } else {
        stubDailyTrainingTimeline().filterIsInstance<TimelineState.Daily.Item>().toPersistentList()
    }
    val period = TrainingsTimelinePeriod.Daily(items = items)
    val range = period.rangeFor(LocalDateTime(2026, 10, 16, 12, 0))

    return TrainingsState(period = period, date = range)
}

private fun monthlyTrainingsPreviewState(empty: Boolean = false): TrainingsState {
    val date = LocalDateTime(2026, 10, 16, 12, 0)
    val range = TrainingsTimelinePeriod.Monthly().rangeFor(date)
    val timeline = if (empty) {
        persistentListOf()
    } else {
        stubMonthlyTrainingTimeline()
    }
    val period = TrainingsTimelinePeriod.Monthly.from(range = range, timeline = timeline)
    val weeks = period.weeks.map { week ->
        week.map { day -> day.copy(isToday = day.date.value == date.date) }.toPersistentList()
    }.toPersistentList()

    return TrainingsState(period = period.copy(weeks = weeks), date = range)
}
