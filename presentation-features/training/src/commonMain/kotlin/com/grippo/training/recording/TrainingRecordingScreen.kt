package com.grippo.training.recording

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.equipments
import com.grippo.state.formatters.UiText
import com.grippo.state.trainings.stubIteration
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingRecordingScreen(
    state: TrainingRecordingState,
    loaders: ImmutableSet<TrainingRecordingLoader>,
    contract: TrainingRecordingContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    val segmentItems = remember {
        persistentListOf(
            RecordingTab.Exercises to UiText.Str("Exercises"),
            RecordingTab.Stats to UiText.Str("Stats"),
        )
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.equipments),
        onBack = contract::onBack,
        content = {
            Segment(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                items = segmentItems,
                selected = state.tab,
                onSelect = contract::onSelectTab
            )
        }
    )

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.content
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        // todo implement content with state.tab
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

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
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