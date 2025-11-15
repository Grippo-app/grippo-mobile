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
import com.grippo.design.components.timeline.TimeLabel
import com.grippo.design.components.timeline.TimelineIndicator
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Menu
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.trainings.factory.timelineStyle
import com.grippo.trainings.trainings.TrainingsContract
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun DailyTrainingsPage(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingListValue>,
    contentPadding: PaddingValues,
    contract: TrainingsContract,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding
    ) {
        if (trainings.isEmpty()) {
            item {
                TrainingsEmptyState(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(vertical = AppTokens.dp.contentPadding.block)
                )
            }
        } else {
            items(
                items = trainings,
                key = { it.key },
                contentType = { it::class }
            ) { value ->
                if (value is TrainingListValue.DailyDigest) {
                    DailyDigestCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = value.state,
                        onViewStatsClick = contract::onDailyDigestViewStats
                    )
                    return@items
                }

                val style = remember(value) { value.timelineStyle() }
                val exercise = remember(value) { value.exercise() }

                TimelineIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    style = style
                ) {
                    if (value is TrainingListValue.DateTime) {
                        val clickProvider = remember(value.trainingId) {
                            { contract.onTrainingMenuClick(value.trainingId) }
                        }

                        Row(
                            modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val time = remember(value.createAt, value.duration) {
                                DateTimeUtils.minus(value.createAt, value.duration)
                            }

                            Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

                            TimeLabel(value = time)

                            Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

                            Text(
                                text = "(${value.duration})",
                                style = AppTokens.typography.h6(),
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
                            { contract.onExerciseClick(exercise.id) }
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
            contract = TrainingsContract.Empty
        )
    }
}
