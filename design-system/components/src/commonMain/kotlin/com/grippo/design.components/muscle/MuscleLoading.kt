package com.grippo.design.components.muscle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.muscle.internal.MuscleLoadingCollapsed
import com.grippo.design.components.muscle.internal.MuscleLoadingExpanded
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
    mode: MuscleLoadingMode = MuscleLoadingMode.Collapsed,
) {
    when (mode) {
        MuscleLoadingMode.Collapsed -> MuscleLoadingCollapsed(
            summary = summary,
            modifier = modifier,
        )

        MuscleLoadingMode.Expanded -> MuscleLoadingExpanded(
            summary = summary,
            modifier = modifier,
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
