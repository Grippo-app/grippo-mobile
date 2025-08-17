package com.grippo.training.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.training.IterationsCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.stubIteration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingRecordingScreen(
    state: TrainingRecordingState,
    loaders: ImmutableSet<TrainingRecordingLoader>,
    contract: TrainingRecordingContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTokens.dp.screen.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Segment(
            items = persistentListOf(
                RecordingTab.Exercises to UiText.Str("Exercises"),
                RecordingTab.Stats to UiText.Str("Stats"),
            ),
            selected = state.tab,
            onSelect = { tab ->
                when (tab) {
                    RecordingTab.Exercises -> contract.onOpenExercises()
                    RecordingTab.Stats -> contract.onOpenStats()
                }
            }
        )
    }

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    when (state.tab) {
        RecordingTab.Exercises -> ExercisesTab(state.exercises, contract)
        RecordingTab.Stats -> StatsTab()
    }

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTokens.dp.screen.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            text = "Add exercise",
            style = ButtonStyle.Secondary,
            onClick = contract::onAddExercise
        )

        Button(
            modifier = Modifier.weight(1f),
            text = "Save",
            style = ButtonStyle.Primary,
            onClick = contract::onSave
        )
    }
}

@Composable
private fun ExercisesTab(exercises: ImmutableList<RecordingExerciseItem>, contract: TrainingRecordingContract) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
    ) {
        exercises.forEach { ex ->
            Text(text = ex.name, style = AppTokens.typography.b14Bold(), color = AppTokens.colors.text.primary)
            Spacer(Modifier.size(AppTokens.dp.contentPadding.content))
            IterationsCard(
                modifier = Modifier.fillMaxWidth(),
                value = ex.iterations
            )
            Spacer(Modifier.size(AppTokens.dp.contentPadding.block))
        }
    }
}

@Composable
private fun StatsTab() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
    ) {
        Text(text = "Summary stats", style = AppTokens.typography.b14Bold(), color = AppTokens.colors.text.primary)
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingRecordingScreen(
            state = TrainingRecordingState(
                exercises = persistentListOf(
                    RecordingExerciseItem(
                        id = "1",
                        name = "Bench Press",
                        iterations = persistentListOf(
                            stubIteration(),
                            stubIteration()
                        )
                    )
                )
            ),
            loaders = persistentSetOf(),
            contract = TrainingRecordingContract.Empty
        )
    }
}