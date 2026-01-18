package com.grippo.exercise.example.exerciseexample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.metrics.stubEstimatedOneRepMax
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.achievement.AchievementsCard
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.example.DescriptionText
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.metrics.EstimatedOneRepMaxCard
import com.grippo.design.components.metrics.MuscleLoading
import com.grippo.design.components.metrics.MuscleLoadingMode
import com.grippo.design.components.metrics.VolumeMetricChart
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.achievements
import com.grippo.design.resources.provider.equipments
import com.grippo.design.resources.provider.history
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
    ) {

        val example = state.example ?: return@BaseComposeScreen

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

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

        Row(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Chip(
                label = ChipLabel.Empty,
                value = example.value.category.title().text(),
                size = ChipSize.Medium,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.static.white,
                brush = SolidColor(example.value.category.color())
            )

            Chip(
                label = ChipLabel.Empty,
                value = example.value.forceType.title().text(),
                size = ChipSize.Medium,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.static.white,
                brush = SolidColor(example.value.forceType.color())
            )

            Chip(
                label = ChipLabel.Empty,
                value = example.value.weightType.title().text(),
                size = ChipSize.Medium,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.static.white,
                brush = SolidColor(example.value.weightType.color())
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        DescriptionText(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            text = example.value.description,
        )

        state.muscleLoad
            ?.takeIf { it.perGroup.entries.isNotEmpty() }
            ?.let { summary ->

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                MuscleLoading(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                        .fillMaxWidth(),
                    summary = summary,
                    mode = MuscleLoadingMode.PerGroup
                )
            }

        if (example.equipments.isNotEmpty()) {

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            ContentSpliter(
                text = AppTokens.strings.res(Res.string.equipments)
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            EquipmentsCard(
                modifier = Modifier.fillMaxWidth(),
                value = example.equipments,
                contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
            )
        }

        if (state.achievements.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                ContentSpliter(
                    text = AppTokens.strings.res(Res.string.achievements)
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                AchievementsCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.achievements,
                    contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
                )

                if (state.estimatedOneRepMax != null) {
                    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                    EstimatedOneRepMaxCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                        state = state.estimatedOneRepMax,
                    )
                }
            }
        }

        if (state.recent.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                ContentSpliter(
                    text = AppTokens.strings.res(Res.string.history)
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                state.exerciseVolume
                    ?.takeIf { it.entries.isNotEmpty() }
                    ?.let { data ->
                        VolumeMetricChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                            value = data,
                        )

                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))
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
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

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
                estimatedOneRepMax = stubEstimatedOneRepMax()
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
