package com.grippo.home.trainings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.border
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.components.timeline.TimeLabel
import com.grippo.design.components.timeline.TimelineIndicator
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.trainings
import com.grippo.domain.mapper.training.transformToTrainingListValue
import com.grippo.home.trainings.factory.exerciseOf
import com.grippo.home.trainings.factory.shapeFor
import com.grippo.home.trainings.factory.sidesFor
import com.grippo.home.trainings.factory.timelineStyle
import com.grippo.presentation.api.trainings.models.TrainingListValue
import com.grippo.presentation.api.trainings.models.stubTraining
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun HomeTrainingsScreen(
    state: HomeTrainingsState,
    loaders: ImmutableSet<HomeTrainingsLoader>,
    contract: HomeTrainingsContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {

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
        items(
            items = state.trainings,
            key = { it.id },
            contentType = { it::class }
        ) { value ->
            val style = remember(value) { timelineStyle(value) }
            val shape = remember(value) { shapeFor(value) }
            val sides = remember(value) { sidesFor(value) }
            val exercise = remember(value) { exerciseOf(value) }

            TimelineIndicator(style = style) {

                if (value is TrainingListValue.DateTime) {
                    TimeLabel(
                        modifier = Modifier.fillMaxWidth(),
                        value = value.date
                    )
                    return@TimelineIndicator
                }

                if (exercise != null) {
                    ExerciseCard(
                        modifier = Modifier
                            .clip(shape)
                            .shadowDefault(
                                shape = shape,
                                elevation = ShadowElevation.Card,
                                sides = sides
                            )
                            .border(
                                width = 1.dp,
                                color = AppTokens.colors.border.defaultPrimary,
                                shape = shape,
                                sides = sides
                            )
                            .background(AppTokens.colors.background.secondary)
                            .fillMaxWidth(),
                        value = exercise,
                        onExerciseExampleClick = contract::openExerciseExample
                    )
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