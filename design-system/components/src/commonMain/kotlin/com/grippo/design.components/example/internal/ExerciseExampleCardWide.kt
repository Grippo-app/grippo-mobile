package com.grippo.design.components.example.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.core.AppTokens
import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.muscles.factory.MuscleColorStrategy
import com.grippo.state.muscles.factory.MuscleEngine
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseExampleCardWide(
    modifier: Modifier,
    value: ExerciseExampleState,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.exerciseExampleCard.wide.radius)

    Column(
        modifier = modifier
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.exerciseExampleCard.wide.horizontalPadding,
                vertical = AppTokens.dp.exerciseExampleCard.wide.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Text(
            text = value.value.name,
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value.value.description,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            Chip(
                modifier = Modifier,
                label = ChipLabel.Empty,
                value = value.value.category.title().text(),
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                modifier = Modifier,
                label = ChipLabel.Empty,
                value = value.value.forceType.title().text(),
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                modifier = Modifier,
                label = ChipLabel.Empty,
                value = value.value.weightType.title().text(),
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                modifier = Modifier,
                label = ChipLabel.Empty,
                value = value.value.experience.title().text(),
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )
        }

        val topBundles = remember(value.bundles) {
            value.bundles
                .sortedByDescending { it.percentage.value }
                .take(3)
                .toPersistentList()
        }

        if (topBundles.isNotEmpty()) {
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
                            label = ChipLabel.Text(item.muscle.type.title()),
                            value = item.percentage.short(),
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

        if (value.equipments.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalAlignment = Alignment.CenterVertically
            ) {
                value.equipments.take(3).forEach { eq ->
                    key(eq.id) {
                        Image(
                            modifier = Modifier.size(AppTokens.dp.overviewCard.icon),
                            imageVector = eq.image(),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Pill(
    modifier: Modifier = Modifier,
    text: String
) {
    Row(
        modifier = modifier
            .background(AppTokens.colors.background.screen, RoundedCornerShape(999.dp))
            .padding(
                horizontal = AppTokens.dp.contentPadding.subContent,
                vertical = AppTokens.dp.contentPadding.text
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.secondary
        )
    }
}

@Composable
private fun MuscleChip(
    bundle: ExerciseExampleBundleState,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(
                Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.7f), color)
                ),
                RoundedCornerShape(999.dp)
            )
            .padding(
                horizontal = AppTokens.dp.contentPadding.content,
                vertical = AppTokens.dp.contentPadding.text
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bundle.muscle.type.title().text(),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.muscle.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = " ${'$'}{bundle.percentage.short()}",
            style = AppTokens.typography.b12Bold(),
            color = AppTokens.colors.muscle.text
        )
    }
}


