package com.grippo.home.trainings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.timeline.TimeLinePointStyle
import com.grippo.design.components.timeline.TimelineIndicator
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.trainings
import com.grippo.domain.mapper.training.transformToTrainingListValue
import com.grippo.presentation.api.trainings.models.TrainingListValue
import com.grippo.presentation.api.trainings.models.TrainingPosition
import com.grippo.presentation.api.trainings.models.stubTraining
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun HomeTrainingsScreen(
    state: HomeTrainingsState,
    loaders: ImmutableSet<HomeTrainingsLoader>,
    contract: HomeTrainingsContract
) = BaseComposeScreen(AppTokens.colors.background.secondary) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        ),
    ) {

        items(state.trainings) { value ->
            val timelineStyle = when (value) {
                is TrainingListValue.DateTime -> when (value.position) {
                    TrainingPosition.FIRST -> TimeLinePointStyle.Start
                    TrainingPosition.MIDDLE -> TimeLinePointStyle.Middle
                    TrainingPosition.LAST -> TimeLinePointStyle.End
                    TrainingPosition.SINGLE -> TimeLinePointStyle.Single
                }

                is TrainingListValue.FirstExercise -> when (value.position) {
                    TrainingPosition.LAST -> TimeLinePointStyle.Empty
                    TrainingPosition.SINGLE -> TimeLinePointStyle.Empty
                    TrainingPosition.FIRST -> TimeLinePointStyle.Line
                    TrainingPosition.MIDDLE -> TimeLinePointStyle.Line
                }

                is TrainingListValue.MiddleExercise -> when (value.position) {
                    TrainingPosition.LAST -> TimeLinePointStyle.Empty
                    TrainingPosition.SINGLE -> TimeLinePointStyle.Empty
                    TrainingPosition.FIRST -> TimeLinePointStyle.Line
                    TrainingPosition.MIDDLE -> TimeLinePointStyle.Line
                }

                is TrainingListValue.LastExercise -> when (value.position) {
                    TrainingPosition.LAST -> TimeLinePointStyle.Empty
                    TrainingPosition.SINGLE -> TimeLinePointStyle.Empty
                    TrainingPosition.FIRST -> TimeLinePointStyle.Line
                    TrainingPosition.MIDDLE -> TimeLinePointStyle.Line
                }

                is TrainingListValue.SingleExercise -> when (value.position) {
                    TrainingPosition.SINGLE -> TimeLinePointStyle.Single
                    TrainingPosition.LAST -> TimeLinePointStyle.End
                    TrainingPosition.FIRST -> TimeLinePointStyle.Start
                    TrainingPosition.MIDDLE -> TimeLinePointStyle.End
                }
            }

            TimelineIndicator(style = timelineStyle) {
                Box(
                    modifier = Modifier.size(100.dp).background(Color.Cyan)
                )
            }

//            ExerciseCard(
//                modifier = Modifier.fillMaxWidth(),
//                value = exercise,
//                onExerciseExampleClick = contract::openExerciseExample
//            )
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