package com.grippo.design.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.ExerciseSpotlightState
import com.grippo.core.state.metrics.stubExerciseSpotlightBestProgress
import com.grippo.core.state.metrics.stubExerciseSpotlightComebackMissing
import com.grippo.core.state.metrics.stubExerciseSpotlightMostConsistent
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun ExerciseSpotlightsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<ExerciseSpotlightState>,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        value.forEach { item ->
            ExerciseSpotlightCard(
                modifier = Modifier,
                value = item
            )
        }
    }
}

@Composable
public fun ExerciseSpotlightCard(
    modifier: Modifier = Modifier,
    value: ExerciseSpotlightState,
) {
    val color = value.color()

    val shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        ExerciseExampleCard(
            modifier = Modifier.weight(1f),
            style = ExerciseExampleCardStyle.Small(
                value = value.example
            )
        )

        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            Text(
                modifier = Modifier
                    .clip(shape)
                    .background(color.copy(alpha = 0.2f), shape = shape)
                    .padding(
                        horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                        vertical = AppTokens.dp.metrics.status.verticalPadding
                    ),
                text = value.title(),
                style = AppTokens.typography.b11Semi(),
                color = color
            )

            Text(
                text = value.description(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseSpotlightCardPreview() {
    PreviewContainer {
        ExerciseSpotlightsCard(
            value = persistentListOf(
                stubExerciseSpotlightMostConsistent(),
                stubExerciseSpotlightBestProgress(),
                stubExerciseSpotlightComebackMissing()
            )
        )
        ExerciseSpotlightCard(
            value = stubExerciseSpotlightMostConsistent(),
        )
    }
}
