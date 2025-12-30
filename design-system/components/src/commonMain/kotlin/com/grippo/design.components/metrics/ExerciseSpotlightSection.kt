package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.ExerciseSpotlight
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_focus_exercise

@Composable
public fun ExerciseSpotlightSection(
    value: ExerciseSpotlight,
    modifier: Modifier = Modifier,
    onExampleClick: (id: String) -> Unit,
) {
    MetricSectionPanel(modifier = modifier) {
        Text(
            text = AppTokens.strings.res(Res.string.highlight_focus_exercise),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val onExampleClickProvider = remember(value.exercise.value.id) {
            { onExampleClick.invoke(value.exercise.value.id) }
        }

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = value.exercise,
            style = ExerciseExampleCardStyle.Medium(
                onClick = onExampleClickProvider,
                allowUsageLabel = true
            )
        )
    }
}
