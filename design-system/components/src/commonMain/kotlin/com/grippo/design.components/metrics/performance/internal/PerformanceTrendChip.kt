package com.grippo.design.components.metrics.performance.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceTrendStatusState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun PerformanceTrendChip(
    modifier: Modifier = Modifier,
    status: PerformanceTrendStatusState,
    reference: PerformanceReferenceState,
    emphasized: Boolean = true,
) {
    val label = status.label(reference) ?: return
    val background = if (emphasized) status.backgroundColor() else AppTokens.colors.semantic.neutralBackground

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
            reference = PerformanceReferenceState.Average,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Improved,
            reference = PerformanceReferenceState.Average,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Stable,
            reference = PerformanceReferenceState.Average,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Declined,
            reference = PerformanceReferenceState.Average,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Improved,
            reference = PerformanceReferenceState.Previous,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Stable,
            reference = PerformanceReferenceState.Previous,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Declined,
            reference = PerformanceReferenceState.Previous,
        )
    }
}

@AppPreview
@Composable
private fun PerformanceTrendChipNeutralPreview() {
    PreviewContainer {
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Record,
            reference = PerformanceReferenceState.Average,
            emphasized = false,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Improved,
            reference = PerformanceReferenceState.Average,
            emphasized = false,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Stable,
            reference = PerformanceReferenceState.Average,
            emphasized = false,
        )
        PerformanceTrendChip(
            status = PerformanceTrendStatusState.Declined,
            reference = PerformanceReferenceState.Previous,
            emphasized = false,
        )
    }
}
