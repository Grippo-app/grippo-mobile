package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExerciseHistory
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_no_sets
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseCardSmall(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    style: ExerciseCardStyle.Small,
) {
    val dimensions = AppTokens.dp.exerciseCard.small
    val shape = RoundedCornerShape(dimensions.radius)

    Column(
        modifier = modifier
            .background(AppTokens.colors.background.card, shape)
            .border(dimensions.borderWidth, AppTokens.colors.border.default, shape)
            .padding(
                horizontal = dimensions.horizontalPadding,
                vertical = dimensions.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = value.createdAt.short(),
                style = AppTokens.typography.h6(),
                color = AppTokens.colors.text.primary,
            )
            style.trailing?.invoke()
        }

        if (value.iterations.isEmpty()) {
            Text(
                text = AppTokens.strings.res(Res.string.exercise_history_no_sets),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        } else {
            val results = value.iterations.map { it.recordedResult() }
            val description = results.joinToString(separator = ", ")
            val separatorColor = AppTokens.colors.text.tertiary

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                value.iterations.forEachIndexed { index, iteration ->
                    key(iteration.id) {
                        Text(
                            text = buildAnnotatedString {
                                append(results[index])
                                if (index < results.lastIndex) {
                                    withStyle(SpanStyle(color = separatorColor)) { append(" ·") }
                                }
                            },
                            style = AppTokens.typography.b14Semi(),
                            color = AppTokens.colors.text.primary,
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseCardSmallPreview() {
    val history = stubExerciseHistory()

    PreviewContainer {
        ExerciseCardSmall(
            value = history[0],
            style = ExerciseCardStyle.Small(
                trailing = {
                    Text(
                        text = "+8%",
                        style = AppTokens.typography.b12Semi(),
                        color = AppTokens.colors.text.secondary,
                    )
                },
            ),
        )
        ExerciseCardSmall(value = history[1], style = ExerciseCardStyle.Small())
        ExerciseCardSmall(value = history[2], style = ExerciseCardStyle.Small())
        ExerciseCardSmall(
            value = history[3].copy(iterations = persistentListOf()),
            style = ExerciseCardStyle.Small(),
        )
        ExerciseCardSmall(
            value = history[3].copy(
                iterations = List(10) { index ->
                    history[3].iterations.first().copy(id = "history-many-$index")
                }.toPersistentList(),
            ),
            style = ExerciseCardStyle.Small(),
        )
    }
}
