package com.grippo.exercise.example.exerciseexample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.achievements.stubAchievements
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.performance.stubEstimatedOneRepMax
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.achievement.AchievementsCard
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chart.internal.BarChartXAxisLabels
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.example.ExampleDescriptionText
import com.grippo.design.components.example.ExampleTypeSection
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.components.metrics.performance.EstimatedOneRepMaxCard
import com.grippo.design.components.metrics.volume.VolumeMetricChart
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.components.segment.Segment
import com.grippo.design.components.segment.SegmentStyle
import com.grippo.design.components.segment.SegmentWidth
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.achievements
import com.grippo.design.resources.provider.history
import com.grippo.design.resources.provider.exercise_example_information
import com.grippo.design.resources.provider.no_data_yet
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    var selectedTab by rememberSaveable(state.example?.value?.id) { mutableStateOf(ExerciseExampleTab.Information) }
    val tabs = remember {
        persistentListOf(
            ExerciseExampleTab.Information to UiText.Res(Res.string.exercise_example_information),
            ExerciseExampleTab.Achievements to UiText.Res(Res.string.achievements),
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

                ExerciseExampleImage(
                    modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                    value = example.value.imageUrl,
                    style = ExerciseExampleImageStyle.LARGE
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

                Text(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                        .fillMaxWidth(),
                    text = example.value.name,
                    style = AppTokens.typography.h1(),
                    color = AppTokens.colors.text.primary,
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

                ExampleTypeSection(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                        .fillMaxWidth(),
                    value = example.value
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                Segment(
                    modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding).fillMaxWidth(),
                    items = tabs,
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    style = SegmentStyle.Outline,
                    segmentWidth = SegmentWidth.EqualFill,
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                when (selectedTab) {
                    ExerciseExampleTab.Information -> {
                        ExampleDescriptionText(
                            modifier = Modifier
                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                                .fillMaxWidth(),
                            text = example.value.description,
                        )

                        if (example.equipments.isNotEmpty()) {

                            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                            EquipmentsCard(
                                modifier = Modifier.fillMaxWidth(),
                                value = example.equipments,
                                contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
                            )
                        }

                        state.muscleLoad
                            ?.takeIf { it.perGroup.entries.isNotEmpty() }
                            ?.let { summary ->

                                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                                MuscleLoading(
                                    modifier = Modifier
                                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                                        .fillMaxWidth(),
                                    summary = summary,
                                    mode = MuscleLoadingMode.PerGroup,
                                )
                            }
                    }
                    ExerciseExampleTab.Achievements -> {
                        if (ExerciseExampleLoader.RecentExercises in loaders || ExerciseExampleLoader.Achievements in loaders) {
                            UserDetailsShimmer()
                        } else {
                            if (state.achievements.isNotEmpty()) {
                                AchievementsCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = state.achievements,
                                    contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
                                )

                            }

                            if (state.estimatedOneRepMax != null) {
                                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                                EstimatedOneRepMaxCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                                    state = state.estimatedOneRepMax,
                                )
                            }
                            if (state.recent.isNotEmpty()) {
                                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                                    text = AppTokens.strings.res(Res.string.history),
                                    style = AppTokens.typography.h4(),
                                    color = AppTokens.colors.text.primary,
                                )

                                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                                state.exerciseVolume
                                    ?.takeIf { it.entries.isNotEmpty() }
                                    ?.let { data ->
                                        VolumeMetricChart(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                                            value = data,
                                            xAxisLabels = BarChartXAxisLabels.WithoutLabels
                                        )

                                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
                                    }

                                state.recent.forEachIndexed { index, item ->
                                    key(item.id) {
                                        ExerciseCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                                            value = item,
                                            style = ExerciseCardStyle.Small,
                                        )
                                    }

                                    if (index < state.recent.lastIndex) {
                                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
                                    }
                                }
                            }

                            if (state.achievements.isEmpty() && state.estimatedOneRepMax == null && state.recent.isEmpty()) {
                                Text(
                                    modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding).fillMaxWidth(),
                                    text = AppTokens.strings.res(Res.string.no_data_yet),
                                    style = AppTokens.typography.b14Med(),
                                    color = AppTokens.colors.text.secondary,
                                )
                            }
                        }
                    }
                }
            }
        },
        bottom = {
            when (val mode = state.mode) {
                ExerciseExampleModeState.Default -> Unit
                is ExerciseExampleModeState.Action -> {
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

@Composable
private fun UserDetailsShimmer() {
    Column(
        modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding).fillMaxWidth(),
    ) {
        repeat(3) { index ->
            if (index > 0) Spacer(Modifier.size(AppTokens.dp.contentPadding.content))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTokens.dp.button.medium.height * 2)
                    .clip(RoundedCornerShape(AppTokens.dp.achievementCard.radius))
                    .background(AppTokens.colors.background.card)
                    .shimmer(),
            )
        }
    }
}

private enum class ExerciseExampleTab { Information, Achievements }

@AppPreview
@Composable
private fun ScreenPreview1() {
    PreviewContainer {
        ExerciseExampleScreen(
            state = ExerciseExampleState(
                example = stubExerciseExample(),
                recent = stubExercises()
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf(),
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview2() {
    PreviewContainer {
        ExerciseExampleScreen(
            state = ExerciseExampleState(
                example = stubExerciseExample(),
                recent = stubExercises(),
                estimatedOneRepMax = stubEstimatedOneRepMax(),
                achievements = stubAchievements()
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf()
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewWithAction() {
    PreviewContainer {
        ExerciseExampleScreen(
            state = ExerciseExampleState(
                mode = ExerciseExampleModeState.Action(title = UiText.Str("Change")),
                example = stubExerciseExample(),
                recent = stubExercises(),
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf()
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewUserDetailsLoading() {
    PreviewContainer {
        UserDetailsShimmer()
    }
}
