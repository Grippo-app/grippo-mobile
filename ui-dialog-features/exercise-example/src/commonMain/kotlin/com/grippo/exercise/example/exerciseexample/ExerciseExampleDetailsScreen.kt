package com.grippo.exercise.example.exerciseexample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.achievements.stubAchievements
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.performance.stubEstimatedOneRepMax
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.achievements
import com.grippo.design.resources.provider.exercise_example_information
import com.grippo.exercise.example.exerciseexample.components.ExerciseExampleDetailsAchievementsContent
import com.grippo.exercise.example.exerciseexample.components.ExerciseExampleDetailsHeader
import com.grippo.exercise.example.exerciseexample.components.ExerciseExampleDetailsInformationContent
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleDetailsScreen(
    state: ExerciseExampleDetailsState,
    loaders: ImmutableSet<ExerciseExampleDetailsLoader>,
    contract: ExerciseExampleDetailsContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    var selectedTab by rememberSaveable(state.example?.value?.id) { mutableStateOf(ExerciseExampleDetailsTabState.Information) }
    val tabs = remember {
        persistentListOf(
            ExerciseExampleDetailsTabState.Information to UiText.Res(Res.string.exercise_example_information),
            ExerciseExampleDetailsTabState.Achievements to UiText.Res(Res.string.achievements),
        )
    }

    val basePadding = PaddingValues(top = AppTokens.dp.dialog.top)

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.dialog,
        content = { containerModifier, resolvedPadding ->
            Column(
                modifier = containerModifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(resolvedPadding),
            ) {
                val example = state.example ?: return@Column

                ExerciseExampleDetailsHeader(
                    modifier = Modifier.fillMaxWidth(),
                    value = example,
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                Segment(
                    modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding).fillMaxWidth(),
                    items = tabs,
                    selected = selectedTab,
                    onSelect = {
                        selectedTab = it
                        contract.onTabSelected(it)
                    },
                    style = SegmentStyle.Outline,
                    segmentWidth = SegmentWidth.EqualFill,
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                when (selectedTab) {
                    ExerciseExampleDetailsTabState.Information -> ExerciseExampleDetailsInformationContent(
                        modifier = Modifier.fillMaxWidth(),
                        example = example,
                        muscleLoad = state.muscleLoad,
                    )

                    ExerciseExampleDetailsTabState.Achievements -> ExerciseExampleDetailsAchievementsContent(
                        modifier = Modifier.fillMaxWidth(),
                        state = state,
                        loaders = loaders,
                    )
                }
            }
        },
        bottom = {
            when (val mode = state.mode) {
                ExerciseExampleDetailsModeState.Default -> Unit
                is ExerciseExampleDetailsModeState.Action -> {
                    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                        content = ButtonContent.Text(text = mode.title.text()),
                        style = ButtonStyle.Secondary,
                        onClick = contract::onAction,
                    )
                }
            }

            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
}

@AppPreview
@Composable
private fun ScreenPreview1() {
    PreviewContainer {
        ExerciseExampleDetailsScreen(
            state = ExerciseExampleDetailsState(
                example = stubExerciseExample(),
                recent = stubExercises()
            ),
            contract = ExerciseExampleDetailsContract.Empty,
            loaders = persistentSetOf(),
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview2() {
    PreviewContainer {
        ExerciseExampleDetailsScreen(
            state = ExerciseExampleDetailsState(
                example = stubExerciseExample(),
                recent = stubExercises(),
                estimatedOneRepMax = stubEstimatedOneRepMax(),
                achievements = stubAchievements()
            ),
            contract = ExerciseExampleDetailsContract.Empty,
            loaders = persistentSetOf()
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewWithAction() {
    PreviewContainer {
        ExerciseExampleDetailsScreen(
            state = ExerciseExampleDetailsState(
                mode = ExerciseExampleDetailsModeState.Action(title = UiText.Str("Change")),
                example = stubExerciseExample(),
                recent = stubExercises(),
            ),
            contract = ExerciseExampleDetailsContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
