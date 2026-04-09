package com.grippo.design.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.ExerciseSpotlightState
import com.grippo.core.state.metrics.stubExerciseSpotlightGoodFrequency
import com.grippo.core.state.metrics.stubExerciseSpotlightNearBest
import com.grippo.core.state.metrics.stubExerciseSpotlightNeedsAttention
import com.grippo.core.state.metrics.stubExerciseSpotlightProgressWin
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.spotlight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun ExerciseSpotlightsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<ExerciseSpotlightState>,
    onExampleClick: (id: String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        Row(
            modifier = Modifier.padding(bottom = AppTokens.dp.contentPadding.text),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.spotlightCard.icon),
                imageVector = AppTokens.icons.Trophy,
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null
            )

            Text(
                text = AppTokens.strings.res(Res.string.spotlight),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        value.forEach { item ->
            val onExampleClickProvider = remember(item.example.id) {
                { onExampleClick.invoke(item.example.id) }
            }

            ExerciseSpotlightCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .scalableClick(onClick = onExampleClickProvider),
                value = item
            )
        }
    }
}

@Composable
private fun ExerciseSpotlightCard(
    modifier: Modifier = Modifier,
    value: ExerciseSpotlightState,
) {
    val color = value.color()
    val shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
        decoration = {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(AppTokens.dp.metrics.status.verticalPadding)
                    .background(color.copy(alpha = 0.7f))
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseExampleCard(
                modifier = Modifier.weight(1f),
                style = ExerciseExampleCardStyle.Small(value = value.example)
            )

            Text(
                modifier = Modifier
                    .clip(shape)
                    .background(color.copy(alpha = 0.20f), shape = shape)
                    .padding(
                        horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                        vertical = AppTokens.dp.metrics.status.verticalPadding
                    ),
                text = value.chipLabel().text(),
                style = AppTokens.typography.b11Semi(),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = value.metricText().text(),
            style = AppTokens.typography.h6(),
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = value.contextText().text(),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = value.actionText().text(),
            style = AppTokens.typography.b11Semi(),
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSpotlightCardPreview() {
    PreviewContainer {
        ExerciseSpotlightsCard(
            value = persistentListOf(
                stubExerciseSpotlightNeedsAttention(),
                stubExerciseSpotlightProgressWin(),
                stubExerciseSpotlightGoodFrequency(),
                stubExerciseSpotlightNearBest(),
            ),
            onExampleClick = {}
        )
        ExerciseSpotlightCard(
            value = stubExerciseSpotlightProgressWin(),
        )
    }
}
