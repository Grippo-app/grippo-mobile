package com.grippo.design.components.training.muscle

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.muscles.metrics.MuscleLoadSummary
import com.grippo.design.components.muscle.MuscleLoading
import com.grippo.design.components.sections.SectionPanel
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_muscle_focus

@Composable
public fun MuscleLoadSection(
    summary: MuscleLoadSummary,
    modifier: Modifier = Modifier,
) {
    SectionPanel(modifier = modifier) {
        Text(
            text = AppTokens.strings.res(Res.string.highlight_muscle_focus),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        MuscleLoading(
            summary = summary,
            maxVisibleEntries = MAX_MUSCLE_SEGMENTS
        )
    }
}

private const val MAX_MUSCLE_SEGMENTS = 4
