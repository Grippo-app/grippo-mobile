package com.grippo.design.components.training.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.metrics.TrainingMetricsSection
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.sets_label

@Composable
internal fun ExerciseCardLarge(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onClick: () -> Unit
) {
    Column(modifier = modifier.scalableClick(onClick = onClick)) {
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

                TrainingMetricsSection(
                    modifier = Modifier.fillMaxWidth(),
                    state = value.metrics,
                    chipSize = ChipSize.Small,
                )

                if (value.iterations.isNotEmpty()) {
                    Text(
                        text = "${AppTokens.strings.res(Res.string.sets_label)} ${value.iterations.size}",
                        style = AppTokens.typography.b14Med(),
                        color = AppTokens.colors.text.tertiary
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
}

@AppPreview
@Composable
private fun ExerciseCardLargePreview() {
    PreviewContainer {
        ExerciseCardLarge(
            value = stubExercise(),
            onClick = {}
        )
    }
}
