package com.grippo.design.components.example.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.exercise.examples.stubExerciseExample
import com.grippo.state.muscles.factory.MuscleColorStrategy
import com.grippo.state.muscles.factory.MuscleEngine
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseExampleCardWide(
    modifier: Modifier,
    value: ExerciseExampleState,
    onCardClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.exerciseExampleCard.wide.radius)

    Column(
        modifier = modifier
            .scalableClick(onClick = onCardClick)
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.exerciseExampleCard.wide.horizontalPadding,
                vertical = AppTokens.dp.exerciseExampleCard.wide.verticalPadding
            ),
    ) {
        Text(
            text = value.value.name,
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            Chip(
                label = ChipLabel.Empty,
                value = value.value.category.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                label = ChipLabel.Empty,
                value = value.value.forceType.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                label = ChipLabel.Empty,
                value = value.value.weightType.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                label = ChipLabel.Empty,
                value = value.value.experience.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            text = value.value.description,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        val topBundles = remember(value.bundles) {
            value.bundles
                .sortedByDescending { it.percentage.value }
                .take(2)
                .toPersistentList()
        }

        if (topBundles.isNotEmpty()) {

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            val preset = MuscleEngine.generatePreset(
                MuscleColorStrategy.ByScaleStops(topBundles)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
            ) {
                topBundles.forEach { item ->
                    val color = remember(item.id) {
                        item.muscle.type.color(preset)
                    }

                    key(item.muscle.id) {
                        Chip(
                            label = ChipLabel.Empty,
                            value = item.muscle.type.title().text(),
                            size = ChipSize.Medium,
                            stype = ChipStype.Default,
                            trailing = ChipTrailing.Empty,
                            contentColor = AppTokens.colors.muscle.text,
                            brush = Brush.horizontalGradient(
                                colors = listOf(color.copy(alpha = 0.7f), color)
                            )
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardWidePreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Wide({}),
        )
    }
}