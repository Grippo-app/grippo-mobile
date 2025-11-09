package com.grippo.training.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.muscles.stubMuscles
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.TimerChip
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.edit_training_title
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.training
import com.grippo.training.recording.pages.ExercisesPage
import com.grippo.training.recording.pages.StatisticsPage
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingRecordingScreen(
    state: TrainingRecordingState,
    loaders: ImmutableSet<TrainingRecordingLoader>,
    contract: TrainingRecordingContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
        spot = ScreenBackground.Spot(
            top = AppTokens.colors.brand.color5,
            bottom = AppTokens.colors.brand.color2
        )
    )
) {
    val exercisesTxt = AppTokens.strings.res(Res.string.exercises)
    val statisticsTxt = AppTokens.strings.res(Res.string.statistics)

    val segmentItems = remember(exercisesTxt, statisticsTxt) {
        persistentListOf(
            RecordingTab.Exercises to UiText.Str(exercisesTxt),
            RecordingTab.Stats to UiText.Str(statisticsTxt),
        )
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
        title = when (state.stage) {
            StageState.Add -> AppTokens.strings.res(Res.string.training)
            StageState.Draft -> AppTokens.strings.res(Res.string.training)
            is StageState.Edit -> AppTokens.strings.res(Res.string.edit_training_title)
        },
        leading = Leading.Back(contract::onBack),
        trailing = {
            val buttonState = remember(loaders, state.exercises) {
                when {
                    state.exercises.isEmpty() -> ButtonState.Disabled
                    else -> ButtonState.Enabled
                }
            }

            Button(
                modifier = Modifier.padding(end = AppTokens.dp.contentPadding.content),
                content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.save_btn)),
                size = ButtonSize.Medium,
                style = ButtonStyle.Transparent,
                state = buttonState,
                onClick = contract::onSave
            )
        },
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
                    onSelect = contract::onSelectTab,
                    style = SegmentStyle.Outline
                )

                Spacer(modifier = Modifier.weight(1f))

                TimerChip(
                    value = state.startAt,
                    size = ChipSize.Medium,
                )
            }
        }
    )

    when (state.tab) {
        RecordingTab.Exercises -> ExercisesPage(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = state,
            contract = contract
        )

        RecordingTab.Stats -> StatisticsPage(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = state,
            contract = contract
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview1() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = stubTraining().exercises,
                tab = RecordingTab.Exercises
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview2() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                stage = StageState.Add,
                exercises = stubTraining().exercises,
                examples = persistentListOf(stubExerciseExample()),
                muscles = stubMuscles(),
                tab = RecordingTab.Stats
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}
