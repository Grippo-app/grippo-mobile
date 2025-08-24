package com.grippo.training.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_exercise_btn
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.icons.NavArrowRight
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.training
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.stubExercises
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingRecordingScreen(
    state: TrainingRecordingState,
    loaders: ImmutableSet<TrainingRecordingLoader>,
    contract: TrainingRecordingContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    val exercisesTxt = AppTokens.strings.res(Res.string.exercises)
    val statisticsTxt = AppTokens.strings.res(Res.string.statistics)

    val segmentItems = remember {
        persistentListOf(
            RecordingTab.Exercises to UiText.Str(exercisesTxt),
            RecordingTab.Stats to UiText.Str(statisticsTxt),
        )
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.training),
        onBack = contract::onBack,
        content = {
            Row(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                Segment(
                    items = segmentItems,
                    selected = state.tab,
                    onSelect = contract::onSelectTab
                )

                Spacer(modifier = Modifier.weight(1f))

                val buttonState = remember(loaders, state.exercises) {
                    when {
                        state.exercises.isEmpty() -> ButtonState.Disabled
                        else -> ButtonState.Enabled
                    }
                }

                Button(
                    modifier = Modifier,
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.save_btn),
                        endIcon = AppTokens.icons.NavArrowRight,
                    ),
                    style = ButtonStyle.Transparent,
                    state = buttonState,
                    onClick = contract::onSave
                )
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.content,
            start = AppTokens.dp.screen.horizontalPadding,
            end = AppTokens.dp.screen.horizontalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        when (state.tab) {
            RecordingTab.Exercises -> {
                items(
                    items = state.exercises,
                    key = { it.id }
                ) { exercise ->
                    val clickProvider = remember(exercise.id) {
                        { contract.onEditExercise(exercise.id) }
                    }

                    ExerciseCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = exercise,
                        onClick = clickProvider
                    )
                }
            }

            RecordingTab.Stats -> {
                item {
                    Text(
                        text = "Statistics will be implemented here",
                        style = AppTokens.typography.b14Med(),
                        color = AppTokens.colors.text.secondary
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(1f),
        content = ButtonContent.Text(
            text = AppTokens.strings.res(Res.string.add_exercise_btn),
        ),
        style = ButtonStyle.Secondary,
        onClick = contract::onAddExercise
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                exercises = stubExercises()
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}