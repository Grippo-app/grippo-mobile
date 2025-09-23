package com.grippo.design.components.example.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.exercise.examples.stubExerciseExample

@Composable
internal fun ExerciseExampleCardSmall(
    modifier: Modifier,
    value: ExerciseExampleState,
    onCardClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppTokens.dp.exerciseExampleCard.small.radius)

    Row(
        modifier = modifier
            .scalableClick(onClick = onCardClick)
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.exerciseExampleCard.small.horizontalPadding,
                vertical = AppTokens.dp.exerciseExampleCard.small.verticalPadding
            )
            .height(intrinsicSize = IntrinsicSize.Min)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        ExerciseExampleImage(
            value = value.value.imageUrl,
            style = ExerciseExampleImageStyle.MEDIUM
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            Text(
                text = value.value.name,
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                Chip(
                    label = ChipLabel.Empty,
                    value = value.value.category.title().text(),
                    size = ChipSize.Small,
                    stype = ChipStype.Default,
                    trailing = ChipTrailing.Empty,
                    contentColor = AppTokens.colors.static.white,
                    brush = SolidColor(value.value.category.color())
                )

                Chip(
                    label = ChipLabel.Empty,
                    value = value.value.forceType.title().text(),
                    size = ChipSize.Small,
                    stype = ChipStype.Default,
                    trailing = ChipTrailing.Empty,
                    contentColor = AppTokens.colors.static.white,
                    brush = SolidColor(value.value.forceType.color())
                )

                Chip(
                    label = ChipLabel.Empty,
                    value = value.value.weightType.title().text(),
                    size = ChipSize.Small,
                    stype = ChipStype.Default,
                    trailing = ChipTrailing.Empty,
                    contentColor = AppTokens.colors.static.white,
                    brush = SolidColor(value.value.weightType.color())
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardSmallPreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Small({}),
        )
    }
}