package com.grippo.design.components.metrics.performance.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun PerformanceTrendChip(
    modifier: Modifier = Modifier,
    status: PerformanceTrendStatusState,
    baseline: PerformanceBaselineState,
) {
    val label = status.label(baseline) ?: return
    val background = status.backgroundColor()

    Text(
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius))
            .padding(
                horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                vertical = AppTokens.dp.metrics.status.verticalPadding
            ),
        text = label,
        style = AppTokens.typography.b11Semi(),
        color = AppTokens.colors.text.primary,
    )
}

@AppPreview
@Composable
private fun PerformanceTrendChipPreview() {
    PreviewContainer {
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Record,
            baseline = PerformanceBaselineState.Average,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Improved,
            baseline = PerformanceBaselineState.Average,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Stable,
            baseline = PerformanceBaselineState.Average,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Declined,
            baseline = PerformanceBaselineState.Average,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Improved,
            baseline = PerformanceBaselineState.Previous,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Stable,
            baseline = PerformanceBaselineState.Previous,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Declined,
            baseline = PerformanceBaselineState.Previous,
        )
    }
}
