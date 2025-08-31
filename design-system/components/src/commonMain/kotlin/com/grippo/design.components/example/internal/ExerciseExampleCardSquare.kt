package com.grippo.design.components.example.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
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
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.NavArrowRight
import com.grippo.design.resources.provider.overview
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.exercise.examples.stubExerciseExample

@Composable
internal fun ExerciseExampleCardSquare(
    modifier: Modifier,
    value: ExerciseExampleState,
    onCardClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.exerciseExampleCard.square.radius)

    Column(
        modifier = modifier
            .scalableClick(onClick = onCardClick)
            .background(AppTokens.colors.background.card, shape)
            .aspectRatio(1f)
            .padding(
                horizontal = AppTokens.dp.exerciseExampleCard.square.horizontalPadding,
                vertical = AppTokens.dp.exerciseExampleCard.square.verticalPadding
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value.value.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onDetailsClick,
                style = ButtonStyle.Transparent,
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.overview),
                    endIcon = AppTokens.icons.NavArrowRight
                ),
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardSquarePreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.size(250.dp),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Square({}, {}),
        )
    }
}