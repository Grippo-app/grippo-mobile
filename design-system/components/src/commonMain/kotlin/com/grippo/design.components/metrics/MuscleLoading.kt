package com.grippo.design.components.metrics

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.metrics.internal.MuscleLoadingPerGroup
import com.grippo.design.components.metrics.internal.MuscleLoadingPerMuscle
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
    AnimatedContent(
        modifier = modifier,
        targetState = mode
    ) { s ->
        when (s) {
            MuscleLoadingMode.PerGroup -> MuscleLoadingPerGroup(
                modifier = Modifier.fillMaxWidth(),
                summary = summary,
            )

            MuscleLoadingMode.PerMuscle -> MuscleLoadingPerMuscle(
                modifier = Modifier.fillMaxWidth(),
                summary = summary,
            )
        }
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
