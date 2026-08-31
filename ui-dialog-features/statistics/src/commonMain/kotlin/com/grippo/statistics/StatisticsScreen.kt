package com.grippo.statistics

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.core.state.metrics.volume.stubVolumeSeries
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun StatisticsScreen(
    state: StatisticsState,
    loaders: ImmutableSet<StatisticsLoader>,
    contract: StatisticsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Trainings(range = DateRangeFormatState.of(DateRangeKind.Weekly)),
                total = stubTotal(),
                exerciseVolume = stubVolumeSeries(),
                muscleLoad = stubMuscleLoadSummary(),
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty
        )
    }
}
