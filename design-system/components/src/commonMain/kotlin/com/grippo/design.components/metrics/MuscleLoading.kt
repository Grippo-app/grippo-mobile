package com.grippo.design.components.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.metrics.internal.MuscleLoadingCollapsed
import com.grippo.design.components.metrics.internal.MuscleLoadingExpanded
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class MuscleLoadingMode {
    Collapsed,
    Expanded,
}

@Composable
public fun MuscleLoading(
    summary: MuscleLoadSummaryState,
    modifier: Modifier = Modifier,
    mode: MuscleLoadingMode,
) {
    when (mode) {
        MuscleLoadingMode.Collapsed -> MuscleLoadingCollapsed(
            modifier = modifier,
            summary = summary,
        )

        MuscleLoadingMode.Expanded -> MuscleLoadingExpanded(
            modifier = modifier,
            summary = summary,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCollapsedPreview() {
    PreviewContainer {
        MuscleLoading(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingMode.Collapsed,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingExpandedPreview() {
    PreviewContainer {
        MuscleLoading(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingMode.Expanded,
        )
    }
}
