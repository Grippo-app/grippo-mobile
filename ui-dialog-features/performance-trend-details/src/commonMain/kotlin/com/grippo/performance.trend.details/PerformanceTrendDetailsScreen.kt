package com.grippo.performance.trend.details

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.stubPerformanceTrendHistory
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun PerformanceTrendDetailsScreen(
    state: PerformanceTrendDetailsDialogState,
    loaders: ImmutableSet<PerformanceTrendDetailsLoader>,
    contract: PerformanceTrendDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        PerformanceTrendDetailsScreen(
            state = PerformanceTrendDetailsDialogState(
                range = DateRangeFormatState.of(DateRangeKind.Last7Days),
                metricType = PerformanceMetricTypeState.Volume,
                history = stubPerformanceTrendHistory(),
            ),
            loaders = persistentSetOf(),
            contract = PerformanceTrendDetailsContract.Empty
        )
    }
}
