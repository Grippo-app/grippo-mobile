package com.grippo.design.components.example.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ExerciseExampleCardMedium(
    modifier: Modifier,
    value: ExerciseExampleState,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
private fun ExerciseExampleCardMediumPreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Medium(
                stubExerciseExample(),
            ),
        )
    }
}