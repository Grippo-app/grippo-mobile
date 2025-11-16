package com.grippo.trainings.trainings.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingListValue.Companion.exercise
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.digest.DailyDigestCard
import com.grippo.design.components.timeline.TimelineIndicator
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Menu
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.trainings.factory.timelineStyle
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun DailyTrainingsPage(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingListValue>,
    contentPadding: PaddingValues,
    onViewStatsClick: () -> Unit,
    onTrainingMenuClick: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()

    val digests = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.DailyDigest>()
    }

    val timelineItems = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.Daily.Item>()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding
    ) {
        if (digests.isEmpty() && timelineItems.isEmpty()) {
            item {
                TrainingsEmptyState(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(vertical = AppTokens.dp.contentPadding.block)
                )
            }
        } else {
            items(digests, key = { it.key }) { digest ->
                DailyDigestCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppTokens.dp.contentPadding.block),
                    value = digest.state,
                    onViewStatsClick = onViewStatsClick
                )
            }

            items(
                items = timelineItems,
                key = { it.key },
                contentType = { it::class }
            ) { value ->
                DailyTimelineItem(
                    value = value,
                    onTrainingMenuClick = onTrainingMenuClick,
                    onExerciseClick = onExerciseClick
                )
            }
        }
    }
}

@Composable
private fun DailyTimelineItem(
    value: TrainingListValue.Daily.Item,
    onTrainingMenuClick: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    val style = remember(value) { value.timelineStyle() }
    val exercise = remember(value) { value.exercise() }

    TimelineIndicator(
        modifier = Modifier.fillMaxWidth(),
        style = style
    ) {
        if (value is TrainingListValue.DateTime) {
            val clickProvider = remember(value.trainingId) {
                { onTrainingMenuClick(value.trainingId) }
            }

            Row(
                modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val startLabel = remember(value.createAt, value.duration) {
                    val v = DateTimeUtils.minus(value.createAt, value.duration)
                    DateTimeUtils.format(v, DateFormat.TIME_24H_H_MM)
                }

                val endLabel = remember(value.createAt) {
                    val v = value.createAt
                    DateTimeUtils.format(v, DateFormat.TIME_24H_H_MM)
                }

                Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

                Text(
                    text = "$startLabel - $endLabel",
                    style = AppTokens.typography.h6(),
                    color = AppTokens.colors.text.primary
                )

                Spacer(Modifier.width(AppTokens.dp.contentPadding.content))

                Text(
                    text = DateTimeUtils.format(value.duration),
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.tertiary
                )

                Spacer(Modifier.weight(1f))

                Button(
                    content = ButtonContent.Icon(
                        icon = AppTokens.icons.Menu,
                    ),
                    style = ButtonStyle.Transparent,
                    size = ButtonSize.Small,
                    onClick = clickProvider
                )
            }
        }

        if (exercise != null) {
            val clickProvider = remember(exercise.id) {
                { onExerciseClick(exercise.id) }
            }

            ExerciseCard(
                modifier = Modifier
                    .padding(vertical = AppTokens.dp.contentPadding.subContent)
                    .fillMaxWidth(),
                value = exercise,
                style = ExerciseCardStyle.Medium(clickProvider)
            )
        }
    }
}

@AppPreview
@Composable
private fun DailyTrainingsPagePreview() {
    PreviewContainer {
        DailyTrainingsPage(
            trainings = listOf(
                stubTraining(),
                stubTraining(),
            ).transformToTrainingListValue(
                range = DateTimeUtils.thisDay()
            ),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onViewStatsClick = {},
            onTrainingMenuClick = {},
            onExerciseClick = {},
        )
    }
}
