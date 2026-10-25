package com.grippo.start.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.start_training_choice_empty
import com.grippo.design.resources.provider.start_training_choice_preset
import com.grippo.design.resources.provider.start_training_choice_recent
import com.grippo.design.resources.provider.start_training_choose_exercises_btn
import com.grippo.design.resources.provider.start_training_repeat_btn
import com.grippo.design.resources.provider.start_training_use_preset_btn
import com.grippo.start.training.components.StartTrainingPage
import com.grippo.start.training.components.StartTrainingRecentPicker
import com.grippo.start.training.components.StartTrainingSkeleton
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

private enum class StartTrainingChoice {
    Empty,
    Preset,
    Recent,
}

@Composable
internal fun StartTrainingScreen(
    state: StartTrainingState,
    loaders: ImmutableSet<StartTrainingLoader>,
    contract: StartTrainingContract,
) {
    var selectedChoice by remember { mutableStateOf(StartTrainingChoice.Empty) }
    var selectedRecentKey by remember { mutableStateOf<String?>(null) }

    StartTrainingContent(
        state = state,
        loaders = loaders,
        selectedChoice = selectedChoice,
        selectedRecentKey = selectedRecentKey,
        onSelectChoice = { selectedChoice = it },
        onSelectRecent = { selectedRecentKey = it },
        contract = contract,
    )
}

@Composable
private fun StartTrainingContent(
    state: StartTrainingState,
    loaders: ImmutableSet<StartTrainingLoader>,
    selectedChoice: StartTrainingChoice,
    selectedRecentKey: String?,
    onSelectChoice: (StartTrainingChoice) -> Unit,
    onSelectRecent: (String) -> Unit,
    contract: StartTrainingContract,
) = BaseComposeScreen(
    background = ScreenBackground.Color(AppTokens.colors.background.dialog),
) {
    val preset = state.options.filterIsInstance<StartTrainingOption.Preset>().firstOrNull()
    val recents = state.options.filterIsInstance<StartTrainingOption.Recent>().toPersistentList()
    val isRefreshing = StartTrainingLoader.Options in loaders
    val isLoading = isRefreshing && preset == null && recents.isEmpty()
    val selectedRecent = recents.firstOrNull { it.key == selectedRecentKey } ?: recents.firstOrNull()
    val option = when (selectedChoice) {
        StartTrainingChoice.Empty -> StartTrainingOption.Empty
        StartTrainingChoice.Preset -> preset
        StartTrainingChoice.Recent -> selectedRecent
    } ?: StartTrainingOption.Empty
    val activeChoice = when (option) {
        StartTrainingOption.Empty -> StartTrainingChoice.Empty
        is StartTrainingOption.Preset -> StartTrainingChoice.Preset
        is StartTrainingOption.Recent -> StartTrainingChoice.Recent
    }
    val choices = buildList<Pair<StartTrainingChoice, UiText>> {
        add(StartTrainingChoice.Empty to UiText.Res(Res.string.start_training_choice_empty))
        if (preset != null) {
            add(StartTrainingChoice.Preset to UiText.Res(Res.string.start_training_choice_preset))
        }
        if (recents.isNotEmpty()) {
            add(StartTrainingChoice.Recent to UiText.Res(Res.string.start_training_choice_recent))
        }
    }.toPersistentList()
    val actionLabel = when (option) {
        StartTrainingOption.Empty -> Res.string.start_training_choose_exercises_btn
        is StartTrainingOption.Preset -> Res.string.start_training_use_preset_btn
        is StartTrainingOption.Recent -> Res.string.start_training_repeat_btn
    }
    val recentPicker: (@Composable () -> Unit)? = if (option is StartTrainingOption.Recent) {
        {
            StartTrainingRecentPicker(
                modifier = Modifier.fillMaxWidth(),
                options = recents,
                selectedKey = option.key,
                onSelect = onSelectRecent,
            )
        }
    } else {
        null
    }

    if (choices.size > 1) {
        Segment(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppTokens.dp.dialog.horizontalPadding,
                    end = AppTokens.dp.dialog.horizontalPadding,
                    top = AppTokens.dp.dialog.top,
                ),
            items = choices,
            style = SegmentStyle.Fill,
            selected = activeChoice,
            onSelect = onSelectChoice,
            segmentWidth = SegmentWidth.EqualFill,
        )
    }

    OverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(weight = 1f, fill = true),
        contentPadding = PaddingValues(
            start = AppTokens.dp.dialog.horizontalPadding,
            end = AppTokens.dp.dialog.horizontalPadding,
            top = if (choices.size > 1) {
                AppTokens.dp.contentPadding.block
            } else {
                AppTokens.dp.dialog.top
            },
            bottom = AppTokens.dp.contentPadding.block,
        ),
        overlay = AppTokens.colors.background.dialog,
        bottom = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = AppTokens.dp.dialog.horizontalPadding,
                        end = AppTokens.dp.dialog.horizontalPadding,
                        top = AppTokens.dp.contentPadding.content,
                        bottom = AppTokens.dp.dialog.bottom,
                    ),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    content = ButtonContent.Text(text = AppTokens.strings.res(actionLabel)),
                    style = ButtonStyle.Primary,
                    onClick = { contract.onSelect(option.key) },
                )
            }
        },
        content = { contentModifier, contentPadding ->
            if (isLoading) {
                StartTrainingSkeleton(
                    modifier = contentModifier.fillMaxSize(),
                    contentPadding = contentPadding,
                )
            } else {
                StartTrainingPage(
                    modifier = contentModifier.fillMaxSize(),
                    option = option,
                    contentPadding = contentPadding,
                    recentPicker = recentPicker,
                )
            }
        },
    )
}

@AppPreview
@Composable
private fun StartTrainingLoadingPreview() {
    PreviewContainer {
        StartTrainingScreen(
            state = StartTrainingState(),
            loaders = persistentSetOf(StartTrainingLoader.Options),
            contract = StartTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingEmptyPreview() {
    PreviewContainer {
        StartTrainingScreen(
            state = StartTrainingState(),
            loaders = persistentSetOf(),
            contract = StartTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingEmptyWithChoicesPreview() {
    PreviewContainer {
        StartTrainingScreen(
            state = startTrainingPreviewState(),
            loaders = persistentSetOf(),
            contract = StartTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPresetPreview() {
    PreviewContainer {
        StartTrainingContent(
            state = startTrainingPreviewState(),
            loaders = persistentSetOf(),
            selectedChoice = StartTrainingChoice.Preset,
            selectedRecentKey = null,
            onSelectChoice = {},
            onSelectRecent = {},
            contract = StartTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingRecentPreview() {
    PreviewContainer {
        StartTrainingContent(
            state = startTrainingPreviewState(),
            loaders = persistentSetOf(),
            selectedChoice = StartTrainingChoice.Recent,
            selectedRecentKey = "recent:evening",
            onSelectChoice = {},
            onSelectRecent = {},
            contract = StartTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingLongPlanPreview() {
    PreviewContainer {
        StartTrainingContent(
            state = startTrainingPreviewState(longContent = true, includeRecent = false),
            loaders = persistentSetOf(),
            selectedChoice = StartTrainingChoice.Preset,
            selectedRecentKey = null,
            onSelectChoice = {},
            onSelectRecent = {},
            contract = StartTrainingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingLongRecentPreview() {
    PreviewContainer {
        StartTrainingContent(
            state = startTrainingPreviewState(longContent = true),
            loaders = persistentSetOf(),
            selectedChoice = StartTrainingChoice.Recent,
            selectedRecentKey = "recent:evening",
            onSelectChoice = {},
            onSelectRecent = {},
            contract = StartTrainingContract.Empty,
        )
    }
}

private fun startTrainingPreviewState(
    longContent: Boolean = false,
    includeRecent: Boolean = true,
): StartTrainingState {
    val names = listOf("Dumbbell bench press", "Lat pulldown", "Leg press", "Seated leg curl")
    val createdAt = DateTimeFormatState.of(
        value = LocalDateTime(year = 2026, month = 10, day = 16, hour = 18, minute = 30),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    val regularExercises = names.mapIndexed { exerciseIndex, name ->
        val base = stubExercise()
        base.copy(
            id = "exercise-$exerciseIndex",
            name = name,
            exerciseExample = base.exerciseExample.copy(
                id = "example-$exerciseIndex",
                name = name,
                imageUrl = null,
            ),
            iterations = List(3) { setIndex ->
                stubPendingIteration().copy(
                    id = "exercise-$exerciseIndex-set-$setIndex",
                    repetitions = when {
                        exerciseIndex == 1 -> RepetitionsFormatState.of(12 - setIndex * 2)
                        exerciseIndex == 2 && setIndex == 1 -> RepetitionsFormatState.Empty()
                        else -> RepetitionsFormatState.of(10)
                    },
                )
            }.toPersistentList(),
            createdAt = createdAt,
        )
    }.toPersistentList()
    val exercises = if (longContent) {
        List(8) { index ->
            val exercise = regularExercises[index % regularExercises.size]
            val name = "Жим гантелей лежачи на похилій лаві нейтральним хватом"
            exercise.copy(
                id = "long-exercise-$index",
                name = name,
                exerciseExample = exercise.exerciseExample.copy(name = name),
                iterations = if (index == 0) persistentListOf() else exercise.iterations,
            )
        }.toPersistentList()
    } else {
        regularExercises
    }
    val options = buildList {
        add(StartTrainingOption.Preset(exercises = exercises))
        add(StartTrainingOption.Empty)
        if (includeRecent) {
            add(StartTrainingOption.Recent(
                trainingId = "evening",
                createdAt = LocalDateTime(year = 2026, month = 10, day = 16, hour = 18, minute = 30),
                exercises = exercises,
            ))
            add(StartTrainingOption.Recent(
                trainingId = "morning",
                createdAt = LocalDateTime(year = 2026, month = 10, day = 16, hour = 8, minute = 15),
                exercises = exercises.take(2).toPersistentList(),
            ))
            add(StartTrainingOption.Recent(
                trainingId = "earlier",
                createdAt = LocalDateTime(year = 2026, month = 10, day = 14, hour = 17, minute = 0),
                exercises = exercises,
            ))
        }
    }.toPersistentList()

    return StartTrainingState(
        options = options,
    )
}
