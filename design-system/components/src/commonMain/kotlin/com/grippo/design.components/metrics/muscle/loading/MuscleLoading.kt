package com.grippo.design.components.metrics.muscle.loading

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.metrics.muscle.loading.internal.MuscleLoadingPerGroup
import com.grippo.design.components.metrics.muscle.loading.internal.MuscleLoadingPerMuscle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class MuscleLoadingMode {
    PerGroup,
    PerMuscle,
}

@Immutable
public enum class MuscleLoadingStyle {
    Expanded,
    Collapsed,
}

@Composable
public fun MuscleLoading(
    summary: MuscleLoadSummaryState,
    modifier: Modifier = Modifier,
    mode: MuscleLoadingMode,
    style: MuscleLoadingStyle
) {
    AnimatedContent(
        modifier = modifier,
        targetState = mode
    ) { s ->
        when (s) {
            MuscleLoadingMode.PerGroup -> MuscleLoadingPerGroup(
                modifier = Modifier.fillMaxWidth(),
                summary = summary,
                style = style
            )

            MuscleLoadingMode.PerMuscle -> MuscleLoadingPerMuscle(
                modifier = Modifier.fillMaxWidth(),
                summary = summary,
                style = style
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
            style = MuscleLoadingStyle.Collapsed
        )
        MuscleLoading(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingMode.PerGroup,
            style = MuscleLoadingStyle.Expanded
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
            style = MuscleLoadingStyle.Collapsed
        )
        MuscleLoading(
            summary = stubMuscleLoadSummary(),
            mode = MuscleLoadingMode.PerMuscle,
            style = MuscleLoadingStyle.Expanded
        )
    }
}
