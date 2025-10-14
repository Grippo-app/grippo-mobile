package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.sets_label

@Composable
internal fun ExerciseCardMedium(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.exerciseCard.medium.radius)
            )
            .padding(
                horizontal = AppTokens.dp.exerciseCard.medium.horizontalPadding,
                vertical = AppTokens.dp.exerciseCard.medium.verticalPadding
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            ExerciseExampleImage(
                value = value.exerciseExample.imageUrl,
                style = ExerciseExampleImageStyle.MEDIUM
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
            ) {
                Text(
                    text = value.name,
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
                    VolumeChip(
                        modifier = Modifier.weight(1f),
                        value = value.metrics.volume,
                        style = VolumeChipStyle.SHORT,
                        size = ChipSize.Small
                    )

                    IntensityChip(
                        modifier = Modifier.weight(1f),
                        value = value.metrics.intensity,
                        style = IntensityChipStyle.SHORT,
                        size = ChipSize.Small
                    )

                    RepetitionsChip(
                        modifier = Modifier.weight(1f),
                        value = value.metrics.repetitions,
                        style = RepetitionsChipStyle.SHORT,
                        size = ChipSize.Small
                    )
                }
            }
        }

        if (value.iterations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            Text(
                text = "${AppTokens.strings.res(Res.string.sets_label)} ${value.iterations.size}",
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.tertiary
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                value.iterations.forEach { iteration ->
                    key(iteration.id) {
                        IterationCard(
                            value = iteration,
                            style = IterationCardStyle.SmallView
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseCardMediumPreview() {
    PreviewContainer {
        ExerciseCardMedium(
            value = stubExercise(),
            onClick = {}
        )
    }
}
