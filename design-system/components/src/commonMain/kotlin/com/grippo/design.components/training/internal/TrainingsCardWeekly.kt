package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.sets
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration

@Composable
internal fun TrainingsCardWeekly(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingState>,
) {
    if (trainings.isEmpty()) return

    Column(modifier = modifier) {
        val grouped = trainings
            .groupBy { DateTimeUtils.format(it.createdAt, DateFormat.WEEKDAY_LONG) }
            .entries
            .sortedByDescending { it.value.first().createdAt }

        grouped.forEachIndexed { groupIndex, entry ->
            val totalDuration = remember(entry.value) {
                entry.value.fold(Duration.ZERO) { acc, training -> acc + training.duration }
            }

            val totalExercises = remember(entry.value) {
                entry.value.sumOf { it.exercises.size }
            }

            val totalSets = remember(entry.value) {
                entry.value.sumOf { training -> training.exercises.sumOf { it.iterations.size } }
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = entry.key,
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.subContent))

            WeeklyDaySummary(
                totalExercises = totalExercises,
                totalSets = totalSets,
                totalDuration = totalDuration
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            entry.value.forEachIndexed { index, training ->
                WeeklyTrainingRow(
                    training = training,
                    showDivider = index != entry.value.lastIndex
                )
            }

            if (groupIndex < grouped.lastIndex) {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))
            }
        }
    }
}

@Composable
private fun WeeklyTrainingRow(
    training: TrainingState,
    showDivider: Boolean,
) {
    val startLabel = remember(training.createdAt, training.duration) {
        val minus = DateTimeUtils.minus(training.createdAt, training.duration)
        DateTimeUtils.format(minus, DateFormat.TIME_24H_H_MM)
    }

    val endLabel = remember(training.createdAt) {
        DateTimeUtils.format(training.createdAt, DateFormat.TIME_24H_H_MM)
    }
    val exercisesCount = remember(training.exercises) {
        training.exercises.size
    }

    val setsCount = remember(training.exercises) {
        training.exercises.sumOf { it.iterations.size }
    }

    Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.subContent))

    Column(modifier = Modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
            ) {
                Text(
                    text = "$startLabel - $endLabel",
                    style = AppTokens.typography.h6(),
                    color = AppTokens.colors.text.primary
                )

                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

                val durationText = remember(training.duration) {
                    DateTimeUtils.format(training.duration)
                }

                Text(
                    text = durationText,
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.subContent))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            WeeklyStatChip(
                modifier = Modifier.weight(1f),
                text = "$exercisesCount ${AppTokens.strings.res(Res.string.exercises)}"
            )

            WeeklyStatChip(
                modifier = Modifier.weight(1f),
                text = "$setsCount ${AppTokens.strings.res(Res.string.sets)}"
            )
        }
    }

    if (showDivider) {
        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))
    }
}

@Composable
private fun WeeklyDaySummary(
    modifier: Modifier = Modifier,
    totalExercises: Int,
    totalSets: Int,
    totalDuration: Duration,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically
    ) {

        WeeklyStatChip(
            modifier = Modifier.weight(1f),
            text = "$totalExercises\n${AppTokens.strings.res(Res.string.exercises)}"
        )

        WeeklyStatChip(
            modifier = Modifier.weight(1f),
            text = "$totalSets\n${AppTokens.strings.res(Res.string.sets)}"
        )

        WeeklyStatChip(
            modifier = Modifier.weight(1f),
            text = "${DateTimeUtils.format(totalDuration)}\n${AppTokens.strings.res(Res.string.duration)}"
        )
    }
}

@Composable
private fun WeeklyStatChip(
    modifier: Modifier = Modifier,
    text: String,
    contentColor: androidx.compose.ui.graphics.Color = AppTokens.colors.text.secondary,
) {
    Text(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.trainingCard.weekly.radius)
            ).padding(
                horizontal = AppTokens.dp.contentPadding.content,
                vertical = AppTokens.dp.contentPadding.text
            ),
        text = text,
        style = AppTokens.typography.b13Med(),
        color = contentColor,
        textAlign = TextAlign.Center
    )
}

@AppPreview
@Composable
private fun TrainingsCardWeeklyPreview() {
    PreviewContainer {
        TrainingsCardWeekly(
            trainings = persistentListOf(stubTraining()),
        )

        TrainingsCardWeekly(
            trainings = persistentListOf(stubTraining(), stubTraining(), stubTraining()),
        )
    }
}
