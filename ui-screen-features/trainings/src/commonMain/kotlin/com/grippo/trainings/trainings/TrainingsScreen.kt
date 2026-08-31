package com.grippo.trainings.trainings

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.stubDailyTrainingTimeline
import com.grippo.core.state.trainings.stubMonthlyTrainingTimeline
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun TrainingsScreen(
    state: TrainingsState,
    loaders: ImmutableSet<TrainingsLoader>,
    contract: TrainingsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

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
