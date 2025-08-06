package com.grippo.home.trainings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.date.utils.DateFormat
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.TonnageChip
import com.grippo.design.components.datetime.DatePicker
import com.grippo.design.components.timeline.TimeLabel
import com.grippo.design.components.timeline.TimelineIndicator
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.selected
import com.grippo.design.resources.trainings
import com.grippo.domain.state.training.transformToTrainingListValue
import com.grippo.home.trainings.factory.exerciseOf
import com.grippo.home.trainings.factory.paddingFor
import com.grippo.home.trainings.factory.shapeFor
import com.grippo.home.trainings.factory.sidesFor
import com.grippo.home.trainings.factory.timelineStyle
import com.grippo.state.trainings.TrainingListValue
import com.grippo.state.trainings.stubTraining
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun HomeTrainingsScreen(
    state: HomeTrainingsState,
    loaders: ImmutableSet<HomeTrainingsLoader>,
    contract: HomeTrainingsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = AppTokens.dp.contentPadding.content,
                        start = AppTokens.dp.screen.horizontalPadding,
                        end = AppTokens.dp.screen.horizontalPadding
                    ),
            ) {
                DatePicker(
                    title = AppTokens.strings.res(Res.string.selected),
                    value = state.date,
                    format = DateFormat.uuuu_MM_d,
                    onClick = contract::selectDate
                )
            }
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        ),
    ) {
        items(
            items = state.trainings,
            key = { it.id },
            contentType = { it::class }
        ) { value ->
            val radius = AppTokens.dp.exerciseCard.radius
            val contentPadding = AppTokens.dp.contentPadding.content
            val style = remember(value) { timelineStyle(value) }
            val shape = remember(value) { shapeFor(value, radius) }
            val paddings = remember(value) { paddingFor(value, contentPadding) }
            remember(value) { sidesFor(value) }
            val exercise = remember(value) { exerciseOf(value) }

            TimelineIndicator(style = style) {

                if (value is TrainingListValue.DateTime) {
                    TimeLabel(
                        modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.content),
                        value = value.date
                    )
                    return@TimelineIndicator
                }

                if (value is TrainingListValue.TrainingSummary) {
                    Column(
                        modifier = Modifier
                            .background(AppTokens.colors.background.card, shape)
                            .fillMaxWidth()
                            .padding(
                                horizontal = AppTokens.dp.contentPadding.content,
                                vertical = AppTokens.dp.contentPadding.content,
                            ),
                        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                    ) {

                        Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
                            TonnageChip(
                                modifier = Modifier.weight(1f),
                                value = value.training.volume
                            )
                            RepetitionsChip(
                                modifier = Modifier.weight(1f),
                                value = value.training.repetitions
                            )
                        }

                        IntensityChip(
                            modifier = Modifier.fillMaxWidth(),
                            value = value.training.intensity
                        )

                    }
                    return@TimelineIndicator
                }

                if (exercise != null) {
                    ExerciseCard(
                        modifier = Modifier
                            .background(AppTokens.colors.background.card, shape)
                            .fillMaxWidth()
                            .padding(paddings),
                        value = exercise,
                        onExerciseExampleClick = contract::openExerciseExample,
                        onExerciseClick = contract::openExercise
                    )
                    return@TimelineIndicator
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        HomeTrainingsScreen(
            state = HomeTrainingsState(
                trainings = persistentListOf(
                    stubTraining(),
                    stubTraining()
                ).transformToTrainingListValue(),
            ),
            loaders = persistentSetOf(),
            contract = HomeTrainingsContract.Empty
        )
    }
}