package com.grippo.home.trainings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonColorTokens
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.datetime.DatePicker
import com.grippo.design.components.timeline.TimeLabel
import com.grippo.design.components.timeline.TimelineIndicator
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Menu
import com.grippo.design.resources.provider.trainings
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.home.trainings.factory.timelineStyle
import com.grippo.state.trainings.TrainingListValue
import com.grippo.state.trainings.TrainingListValue.Companion.exercise
import com.grippo.state.trainings.TrainingListValue.Companion.shape
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                DatePicker(
                    value = state.date.from,
                    limitations = state.limitations,
                    format = DateFormat.DATE_MMM_DD_YYYY,
                    onClick = contract::onSelectDate,
                    onPreviousClick = contract::onPreviousClick,
                    onNextClick = contract::onNextClick
                )
            }
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(
            start = AppTokens.dp.screen.horizontalPadding,
            end = AppTokens.dp.screen.horizontalPadding,
            bottom = AppTokens.dp.contentPadding.content
        ),
    ) {
        items(
            items = state.trainings,
            key = { it.key },
            contentType = { it::class }
        ) { value ->
            val radius = AppTokens.dp.menu.radius
            val contentPadding = AppTokens.dp.contentPadding.content
            val style = remember(value) { value.timelineStyle() }
            val shape = remember(value) { value.shape(radius) }
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
                        modifier = Modifier.padding(vertical = contentPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val time = remember(value.createAt, value.duration) {
                            DateTimeUtils.minus(value.createAt, value.duration)
                        }

                        TimeLabel(
                            value = time
                        )

                        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

                        Text(
                            text = "(${value.duration})",
                            style = AppTokens.typography.b14Bold(),
                            color = AppTokens.colors.text.tertiary
                        )

                        Spacer(Modifier.weight(1f))

                        Button(
                            content = ButtonContent.Icon(
                                icon = AppTokens.icons.Menu,
                            ),
                            style = ButtonStyle.Custom(
                                enabled = ButtonColorTokens(
                                    background1 = Color.Transparent,
                                    background2 = Color.Transparent,
                                    content = AppTokens.colors.text.primary,
                                    border = Color.Transparent,
                                    icon = AppTokens.colors.icon.primary,
                                ),
                                disabled = ButtonColorTokens(
                                    background1 = Color.Transparent,
                                    background2 = Color.Transparent,
                                    content = AppTokens.colors.text.disabled,
                                    border = Color.Transparent,
                                    icon = AppTokens.colors.icon.disabled
                                ),
                            ),
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
                            .background(AppTokens.colors.background.card, shape)
                            .fillMaxWidth(),
                        value = exercise,
                        style = ExerciseCardStyle.Small(clickProvider)
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
