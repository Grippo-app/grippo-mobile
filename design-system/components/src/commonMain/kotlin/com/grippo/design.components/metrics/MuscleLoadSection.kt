package com.grippo.design.components.metrics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.MuscleLoadSummary
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_muscle_focus

@Composable
public fun MuscleLoadSection(
    summary: MuscleLoadSummary,
    modifier: Modifier = Modifier,
) {
    MetricSectionPanel(modifier = modifier) {
        Text(
            text = AppTokens.strings.res(Res.string.highlight_muscle_focus),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        MuscleLoading(
            summary = summary,
            maxVisibleEntries = 4
        )
    }
}