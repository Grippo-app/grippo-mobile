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
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.stubDailyTrainingTimeline
import com.grippo.core.state.trainings.stubMonthlyTrainingTimeline
import com.grippo.design.components.datetime.DatePicker
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
                style = SegmentStyle.Fill
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            DatePicker(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                value = state.date.from,
                format = when (state.period) {
                    is TrainingsTimelinePeriod.Daily -> DateFormat.DateOnly.DateDdMmm
                    is TrainingsTimelinePeriod.Monthly -> DateFormat.DateOnly.Mmmm
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

    when (val period = state.period) {
        is TrainingsTimelinePeriod.Daily -> DailyTrainingsPage(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding(),
            items = period.items,
            loaders = loaders,
            contentPadding = basePadding,
            onTrainingMenuClick = contract::onTrainingMenuClick,
            onExerciseClick = contract::onExerciseClick,
        )

        is TrainingsTimelinePeriod.Monthly -> MonthlyTrainingsPage(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding(),
            period = period,
            contentPadding = basePadding,
            onDigestClick = contract::onDailyDigestViewStats,
            onOpenDaily = contract::onOpenDaily,
        )
    }
}

@AppPreview
@Composable
private fun DailyScreenPreview() {
    PreviewContainer {
        val dailyItems = stubDailyTrainingTimeline()
            .filterIsInstance<TimelineState.Daily.Item>()
            .toPersistentList()
        val daily = TrainingsTimelinePeriod.Daily(items = dailyItems)
        TrainingsScreen(
            state = TrainingsState(
                period = daily,
                date = daily.defaultRange(),
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
        val monthlyRange = TrainingsTimelinePeriod.Monthly().defaultRange()
        val monthly = TrainingsTimelinePeriod.Monthly.from(
            range = monthlyRange,
            timeline = stubMonthlyTrainingTimeline(),
        )
        TrainingsScreen(
            state = TrainingsState(
                period = monthly,
                date = monthlyRange,
            ),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty
        )
    }
}
