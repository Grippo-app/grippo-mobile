package com.grippo.design.components.metrics.distribution.muscle.loading

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.MuscleLoadingPerGroup
import com.grippo.design.components.metrics.distribution.muscle.loading.internal.MuscleLoadingPerMuscle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class MuscleLoadingMode {
    PerGroup,
    PerMuscle,
}

@Composable
public fun MuscleLoading(
    summary: MuscleLoadSummaryState,
    modifier: Modifier = Modifier,
    mode: MuscleLoadingMode,
) {
    when (mode) {
        MuscleLoadingMode.PerGroup -> MuscleLoadingPerGroup(
            modifier = modifier.fillMaxWidth(),
            summary = summary,
        )

        MuscleLoadingMode.PerMuscle -> MuscleLoadingPerMuscle(
            modifier = modifier.fillMaxWidth(),
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
            mode = MuscleLoadingMode.PerGroup,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingExpandedPreview() {
    PreviewContainer {
        MuscleLoading(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingMode.PerMuscle,
        )
    }
}
