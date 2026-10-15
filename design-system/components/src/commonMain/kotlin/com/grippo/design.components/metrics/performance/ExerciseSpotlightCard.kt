package com.grippo.design.components.metrics.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.grippo.core.state.metrics.performance.ExerciseSpotlightState
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightGoodFrequency
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightNearBest
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightNeedsAttention
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightProgressWin
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun ExerciseSpotlightsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<ExerciseSpotlightState>,
    onExampleClick: (id: String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val layoutDirection = LocalLayoutDirection.current
    val horizontalPadding = contentPadding.calculateStartPadding(layoutDirection) +
            contentPadding.calculateEndPadding(layoutDirection)

    BoxWithConstraints(modifier = modifier) {
        val cardWidth = if (value.size == 1) {
            (maxWidth - horizontalPadding).coerceAtLeast(0.dp)
        } else {
            AppTokens.dp.metrics.overview.insightCardWidth
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Min)
                .padding(contentPadding)
                .semantics {
                    collectionInfo = CollectionInfo(rowCount = 1, columnCount = value.size)
                },
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            value.forEach { item ->
                key(item.example.id, item.kind) {
                    val onExampleClickProvider = remember(onExampleClick, item.example.id) {
                        { onExampleClick.invoke(item.example.id) }
                    }

                    ExerciseSpotlightCard(
                        modifier = Modifier
                            .width(cardWidth)
                            .fillMaxHeight()
                            .scalableClick(onClick = onExampleClickProvider),
                        value = item,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseSpotlightCard(
    modifier: Modifier = Modifier,
    value: ExerciseSpotlightState,
) {
    val statusBackground = when (value.kind) {
        ExerciseSpotlightState.Kind.ProgressWin,
        ExerciseSpotlightState.Kind.NearBest -> AppTokens.colors.semantic.successBackground

        ExerciseSpotlightState.Kind.NeedsAttention -> AppTokens.colors.semantic.errorBackground
        ExerciseSpotlightState.Kind.GoodFrequency -> AppTokens.colors.semantic.infoBackground
    }
    val shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius)

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseExampleImage(
                value = value.example.imageUrl,
                style = ExerciseExampleImageStyle.MEDIUM,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                MetricOverviewHeader(title = value.example.name)

                Text(
                    modifier = Modifier
                        .clip(shape)
                        .background(statusBackground, shape = shape)
                        .padding(
                            horizontal = AppTokens.dp.metrics.status.horizontalPadding,
                            vertical = AppTokens.dp.metrics.status.verticalPadding,
                        ),
                    text = value.chipLabel().text(),
                    style = AppTokens.typography.b11Semi(),
                    color = AppTokens.colors.text.primary,
                )
            }
        }

        Text(
            text = value.metricText().text(),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = value.contextText().text(),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Text(
            text = value.actionText().text(),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSpotlightCardPreview() {
    PreviewContainer {
        ExerciseSpotlightsCard(
            value = exerciseSpotlightPreviewValues(),
            contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding),
            onExampleClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSpotlightProgressPreview() {
    PreviewContainer {
        ExerciseSpotlightsCard(
            value = persistentListOf(exerciseSpotlightPreviewValues()[1]),
            onExampleClick = {},
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSpotlightAttentionPreview() {
    PreviewContainer {
        ExerciseSpotlightsCard(
            value = persistentListOf(exerciseSpotlightPreviewValues()[0]),
            onExampleClick = {},
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSpotlightFrequencyPreview() {
    PreviewContainer {
        ExerciseSpotlightsCard(
            value = persistentListOf(exerciseSpotlightPreviewValues()[2]),
            onExampleClick = {},
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSpotlightNearBestPreview() {
    PreviewContainer {
        ExerciseSpotlightsCard(
            value = persistentListOf(exerciseSpotlightPreviewValues()[3]),
            onExampleClick = {},
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSpotlightEmptyPreview() {
    PreviewContainer {
        ExerciseSpotlightsCard(
            value = persistentListOf(),
            onExampleClick = {},
        )
    }
}

private fun exerciseSpotlightPreviewValues(): ImmutableList<ExerciseSpotlightState> {
    val values = listOf(
        stubExerciseSpotlightNeedsAttention(),
        stubExerciseSpotlightProgressWin(),
        stubExerciseSpotlightGoodFrequency(),
        stubExerciseSpotlightNearBest(),
    )
    val names = listOf(
        "Single-arm reverse-grip overhead cable triceps extension",
        "Bench press",
        "Seated dumbbell lateral raise",
        "Row",
    )
    return values.mapIndexed { index, value ->
        value.copy(
            example = value.example.copy(
                id = "exercise-spotlight-preview-$index",
                name = names[index],
                imageUrl = null,
            ),
        )
    }.toPersistentList()
}
