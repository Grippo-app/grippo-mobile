package com.grippo.design.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.PerformanceTrendStatusState
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.metrics.stubPerformanceMetrics
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_type_comeback
import com.grippo.design.resources.provider.highlight_type_comeback_hint
import com.grippo.design.resources.provider.highlight_type_consistency
import com.grippo.design.resources.provider.highlight_type_consistency_hint
import com.grippo.design.resources.provider.highlight_type_momentum
import com.grippo.design.resources.provider.highlight_type_momentum_hint
import com.grippo.design.resources.provider.highlights
import com.grippo.design.resources.provider.icons.Intensity

@Composable
public fun HighlightsHeader(
    modifier: Modifier = Modifier,
    streak: TrainingStreakState,
    performance: List<PerformanceMetricState>,
) {
    val storyType = run {
        val dominantMetric = performance.firstOrNull()
        val streakLength = streak.featured.length
        when {
            streakLength >= 3 -> HighlightStoryType.Consistency
            dominantMetric?.status == PerformanceTrendStatusState.Record ||
                    dominantMetric?.status == PerformanceTrendStatusState.Improved ->
                HighlightStoryType.Momentum

            else -> HighlightStoryType.Comeback
        }
    }

    val storyTitle = when (storyType) {
        HighlightStoryType.Consistency ->
            AppTokens.strings.res(Res.string.highlight_type_consistency)

        HighlightStoryType.Momentum ->
            AppTokens.strings.res(Res.string.highlight_type_momentum)

        HighlightStoryType.Comeback ->
            AppTokens.strings.res(Res.string.highlight_type_comeback)
    }
    val storyHint = when (storyType) {
        HighlightStoryType.Consistency ->
            AppTokens.strings.res(Res.string.highlight_type_consistency_hint)

        HighlightStoryType.Momentum ->
            AppTokens.strings.res(Res.string.highlight_type_momentum_hint)

        HighlightStoryType.Comeback ->
            AppTokens.strings.res(Res.string.highlight_type_comeback_hint)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.highlights.icon),
            imageVector = AppTokens.icons.Intensity,
            tint = AppTokens.colors.semantic.warning,
            contentDescription = null
        )

        Text(
            modifier = Modifier.weight(1f),
            text = AppTokens.strings.res(Res.string.highlights),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.semantic.warning,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier
                .background(
                    AppTokens.colors.text.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)
                )
                .padding(
                    horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                    vertical = AppTokens.dp.metrics.status.verticalPadding
                ),
            text = "$storyTitle · $storyHint",
            style = AppTokens.typography.b12Semi(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class HighlightStoryType {
    Consistency,
    Momentum,
    Comeback
}

@AppPreview
@Composable
private fun HighlightsHeaderPreview() {
    PreviewContainer {
        HighlightsHeader(
            streak = stubTrainingStreaks().first(),
            performance = stubPerformanceMetrics(),
        )
    }
}
