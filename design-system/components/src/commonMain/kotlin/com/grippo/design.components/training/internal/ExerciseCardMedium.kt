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
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseCardMedium(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onClick: () -> Unit
) {
    Column(modifier = modifier.scalableClick(onClick = onClick)) {
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
                val total = value.iterations.size
                val pendingCount = value.iterations.count { it.isPending }
                val done = total - pendingCount
                val hasPending = pendingCount > 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = value.exerciseExample.name,
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

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
                    }
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

@AppPreview
@Composable
private fun ExerciseCardMediumPendingPreview() {
    PreviewContainer {
        val base = stubExercise()
        val mixed = (base.iterations.take(2) + List(3) { stubPendingIteration() })
            .toPersistentList()
        ExerciseCardMedium(
            value = base.copy(iterations = mixed),
            onClick = {}
        )
    }
}
