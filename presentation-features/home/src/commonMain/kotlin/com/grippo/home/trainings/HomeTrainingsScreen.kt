package com.grippo.home.trainings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.date.utils.DateFormat
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.datetime.DatePicker
import com.grippo.design.components.menu.MenuCard
import com.grippo.design.components.menu.MenuTrailing
import com.grippo.design.components.timeline.TimeLabel
import com.grippo.design.components.timeline.TimelineIndicator
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.selected
import com.grippo.design.resources.trainings
import com.grippo.domain.state.training.transformToTrainingListValue
import com.grippo.home.trainings.factory.exerciseOf
import com.grippo.home.trainings.factory.paddingFor
import com.grippo.home.trainings.factory.shapeFor
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
            remember(value) { paddingFor(value, contentPadding) }
            val exercise = remember(value) { exerciseOf(value) }

            TimelineIndicator(style = style) {

                if (value is TrainingListValue.DateTime) {
                    Row(
                        modifier = Modifier.padding(vertical = contentPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(contentPadding)
                    ) {
                        TimeLabel(value = value.date)

                        Spacer(Modifier.weight(1f))

                        VolumeChip(
                            value = value.volume,
                            style = VolumeChipStyle.SHORT
                        )
                    }
                }

                if (exercise != null) {
                    val clickProvider = remember {
                        { contract.openExercise(exercise.id) }
                    }
                    MenuCard(
                        modifier = Modifier
                            .background(AppTokens.colors.background.card, shape)
                            .fillMaxWidth(),
                        title = exercise.name,
                        trailing = MenuTrailing.Text(UiText.Str("1.")),
                        onClick = clickProvider
                    )
                }

                if (value is TrainingListValue.BetweenExercises) {
                    HorizontalDivider(
                        modifier = Modifier
                            .background(AppTokens.colors.background.card, shape)
                            .padding(horizontal = AppTokens.dp.menu.item.horizontalPadding)
                            .fillMaxWidth(),
                        color = AppTokens.colors.divider.default
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