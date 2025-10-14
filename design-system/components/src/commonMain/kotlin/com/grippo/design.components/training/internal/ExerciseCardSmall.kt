package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ExerciseCardSmall(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.exerciseCard.small.radius)
            )
            .padding(
                horizontal = AppTokens.dp.exerciseCard.small.horizontalPadding,
                vertical = AppTokens.dp.exerciseCard.small.verticalPadding
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {

            ExerciseExampleImage(
                value = value.exerciseExample.imageUrl,
                style = ExerciseExampleImageStyle.MEDIUM
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
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
}

@AppPreview
@Composable
private fun ExerciseCardSmallPreview() {
    PreviewContainer {
        ExerciseCardSmall(
            value = stubExercise(),
            onClick = {}
        )
    }
}
