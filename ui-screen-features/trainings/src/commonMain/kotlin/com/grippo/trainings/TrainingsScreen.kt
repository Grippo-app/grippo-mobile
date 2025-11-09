package com.grippo.trainings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingListValue.Companion.exercise
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.datetime.DatePicker
import com.grippo.design.components.frames.BottomOverlayLazyColumn
import com.grippo.design.components.timeline.TimeLabel
import com.grippo.design.components.timeline.TimelineIndicator
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Gym
import com.grippo.design.resources.provider.icons.Menu
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.start_workout
import com.grippo.design.resources.provider.trainings
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.trainings.factory.timelineStyle
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingsScreen(
    state: TrainingsState,
    loaders: ImmutableSet<TrainingsLoader>,
    contract: TrainingsContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
        spot = ScreenBackground.Spot(
            top = AppTokens.colors.brand.color5,
            bottom = AppTokens.colors.brand.color5
        )
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
        style = ToolbarStyle.Transparent,
        trailing = {
            Button(
                modifier = Modifier.padding(end = AppTokens.dp.contentPadding.subContent),
                content = ButtonContent.Icon(icon = AppTokens.icons.User),
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                onClick = contract::onOpenProfile
            )
        },
        content = {
            DatePicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppTokens.dp.screen.horizontalPadding,
                        end = AppTokens.dp.screen.horizontalPadding,
                        bottom = AppTokens.dp.contentPadding.content,
                    ),
                value = state.date.from,
                limitations = state.limitations,
                format = DateFormat.DATE_MMM_DD_YYYY,
                onClick = contract::onSelectDate,
            )
        }
    )

    BottomOverlayLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppTokens.dp.screen.horizontalPadding,
            end = AppTokens.dp.screen.horizontalPadding,
            bottom = AppTokens.dp.contentPadding.content
        ),
        overlay = AppTokens.colors.background.screen,
        content = {
            items(
                items = state.trainings,
                key = { it.key },
                contentType = { it::class }
            ) { value ->
                val style = remember(value) { value.timelineStyle() }
                val exercise = remember(value) { value.exercise() }

                TimelineIndicator(
                    modifier = Modifier.animateItem(),
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

                            TimeLabel(
                                value = time
                            )

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
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(1f),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_workout),
                    startIcon = AppTokens.icons.Gym
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onAddTraining
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingsScreen(
            state = TrainingsState(
                trainings = persistentListOf(
                    stubTraining(),
                    stubTraining()
                ).transformToTrainingListValue(),
            ),
            loaders = persistentSetOf(),
            contract = TrainingsContract.Empty
        )
    }
}
