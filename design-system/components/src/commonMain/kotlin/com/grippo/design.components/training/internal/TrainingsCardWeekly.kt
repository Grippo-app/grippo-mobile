package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.trainingCard.weekly.radius))
            .background(AppTokens.colors.background.card)
            .padding(AppTokens.dp.contentPadding.block)
    ) {
        val grouped = trainings
            .groupBy {
                DateTimeUtils.format(it.createdAt, DateFormat.WEEKDAY_LONG)
            }
            .entries
            .sortedByDescending { it.value.first().createdAt }

        grouped.forEachIndexed { groupIndex, entry ->
            Text(
                text = entry.key,
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary
            )

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

    Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

    Text(
        text = "$startLabel - $endLabel",
        style = AppTokens.typography.h6(),
        color = AppTokens.colors.text.secondary
    )

    Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

    WeeklyTrainingStats(
        duration = training.duration,
        exercises = exercisesCount,
        sets = setsCount,
        volume = training.metrics.volume
    )

    if (showDivider) {
        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))
    }
}

@Composable
private fun WeeklyTrainingStats(
    duration: Duration,
    exercises: Int,
    sets: Int,
    volume: com.grippo.core.state.formatters.VolumeFormatState,
) {
    Text(
        text = "$exercises ${AppTokens.strings.res(Res.string.exercises)} · $sets ${
            AppTokens.strings.res(
                Res.string.sets
            )
        } · ${AppTokens.strings.res(Res.string.duration)} $duration",
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.tertiary
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
