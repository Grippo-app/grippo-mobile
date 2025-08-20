package com.grippo.design.components.training

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
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.sets_label
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.exerciseCard.radius)
            )
            .padding(
                horizontal = AppTokens.dp.exerciseCard.horizontalPadding,
                vertical = AppTokens.dp.exerciseCard.verticalPadding
            )
    ) {
        Text(
            text = value.name,
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.exerciseCard.spacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.exerciseCard.spacing)
        ) {
            VolumeChip(
                value = value.volume,
                style = VolumeChipStyle.SHORT,
                modifier = Modifier.weight(1f)
            )

            IntensityChip(
                value = value.intensity,
                style = IntensityChipStyle.SHORT,
            )

            RepetitionsChip(
                value = value.repetitions,
                style = RepetitionsChipStyle.SHORT,
            )
        }

        if (value.iterations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppTokens.dp.exerciseCard.spacing))

            Text(
                text = "${AppTokens.strings.res(Res.string.sets_label)} ${value.iterations.size}",
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.exerciseCard.spacing))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
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
private fun ExerciseCardPreview() {
    PreviewContainer {
        ExerciseCard(
            value = stubExercise(),
            onClick = {}
        )
    }
}
