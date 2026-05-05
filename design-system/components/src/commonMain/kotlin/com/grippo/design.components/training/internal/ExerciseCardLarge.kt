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
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.metrics.volume.TrainingTotalSection
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.sets_label
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseCardLarge(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
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
                    text = value.exerciseExample.name,
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                TrainingTotalSection(
                    modifier = Modifier.fillMaxWidth(),
                    value = value.total,
                    size = ChipSize.Small,
                )

                if (value.iterations.isNotEmpty()) {
                    val total = value.iterations.size
                    val pendingCount = value.iterations.count { it.isPending }
                    val done = total - pendingCount
                    val hasPending = pendingCount > 0

                    if (hasPending) {
                        val badgeColor = if (done == total) {
                            AppTokens.colors.semantic.success
                        } else {
                            AppTokens.colors.semantic.warning
                        }
                        Text(
                            text = "$done/$total",
                            style = AppTokens.typography.b14Med(),
                            color = badgeColor,
                        )
                    } else {
                        Text(
                            text = "${AppTokens.strings.res(Res.string.sets_label)} $total",
                            style = AppTokens.typography.b14Med(),
                            color = AppTokens.colors.text.tertiary,
                        )
                    }

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

@AppPreview
@Composable
private fun ExerciseCardLargePendingPreview() {
    PreviewContainer {
        val base = stubExercise()
        val mixedIterations = (base.iterations.take(2) + List(3) { stubPendingIteration() })
            .toPersistentList()
        ExerciseCardLarge(
            value = base.copy(iterations = mixedIterations),
            onClick = {}
        )
    }
}
