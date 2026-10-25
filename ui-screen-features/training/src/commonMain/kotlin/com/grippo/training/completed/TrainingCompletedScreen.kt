package com.grippo.training.completed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.core.state.metrics.performance.PerformanceVerdictState
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsByType
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsDeclined
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsImproved
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsWithoutBaseline
import com.grippo.core.state.metrics.performance.stubTrainingRecords
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.core.state.metrics.volume.stubWeeklyVolume
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.konfetti.KonfettiParade
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCard
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardStyle
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCard
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardSkeleton
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardStyle
import com.grippo.design.components.metrics.performance.PerformanceComparisonCard
import com.grippo.design.components.metrics.performance.PerformanceComparisonCardSkeleton
import com.grippo.design.components.metrics.performance.TrainingRecordsCard
import com.grippo.design.components.metrics.performance.TrainingRecordsCardSkeleton
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalCardStyle
import com.grippo.design.components.metrics.volume.WeeklyVolumeCard
import com.grippo.design.components.metrics.volume.WeeklyVolumeCardSkeleton
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.training.TrainingRecapActions
import com.grippo.design.components.training.TrainingRecapHero
import com.grippo.design.components.training.TrainingRecapHeroStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.joined_pair
import com.grippo.design.resources.provider.loading
import com.grippo.design.resources.provider.training_completed_details_unavailable
import com.grippo.design.resources.provider.training_completed_done
import com.grippo.design.resources.provider.training_completed_result_unavailable
import com.grippo.design.resources.provider.workout_summary
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.training.completed.components.TrainingCompletedSkeleton
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun TrainingCompletedScreen(
    state: TrainingCompletedState,
    loaders: ImmutableSet<TrainingCompletedLoader>,
    contract: TrainingCompletedContract,
) = BaseComposeScreen(
    background = ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    ),
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
        title = AppTokens.strings.res(Res.string.workout_summary),
    )

    val basePadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        end = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content,
        bottom = AppTokens.dp.contentPadding.subContent,
    )

    val isSaving = loaders.contains(TrainingCompletedLoader.SaveTraining)

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        content = { containerModifier, resolvedPadding ->
            Box(modifier = containerModifier.fillMaxSize()) {
                if (isSaving) {
                    TrainingCompletedSkeleton(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = resolvedPadding,
                    )
                } else {
                    TrainingCompletedContent(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = resolvedPadding,
                        state = state,
                        loaders = loaders,
                        contract = contract,
                    )
                }

                KonfettiParade(
                    show = state.recordCelebration == RecordCelebrationState.Playing && !isSaving,
                    onShown = contract::onRecordConfettiShown,
                )
            }
        },
        overlay = AppTokens.colors.background.screen,
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.training_completed_done),
                ),
                style = ButtonStyle.Primary,
                state = if (isSaving) ButtonState.Loading else ButtonState.Enabled,
                onClick = contract::onBack,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@Composable
private fun TrainingCompletedContent(
    modifier: Modifier,
    contentPadding: PaddingValues,
    state: TrainingCompletedState,
    loaders: ImmutableSet<TrainingCompletedLoader>,
    contract: TrainingCompletedContract,
) {
    val training = state.training
    val performance = state.performance
    val isLoadingRecords = loaders.contains(TrainingCompletedLoader.LoadRecords)
    val isSharing = loaders.contains(TrainingCompletedLoader.ShareRecap)
    val loadingDescription = AppTokens.strings.res(Res.string.loading)

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        if (training == null) {
            item(key = "completion_unavailable") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(
                        if (state.savedTrainingId != null) {
                            Res.string.training_completed_details_unavailable
                        } else {
                            Res.string.training_completed_result_unavailable
                        },
                    ),
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            return@LazyColumn
        }

        item(key = "completion_hero") {
            TrainingCompletedHero(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                training = training,
                isSharing = isSharing,
                onStatisticsClick = contract::onSummaryClick,
                onShareClick = contract::onShareClick,
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))
        }

        if (isLoadingRecords || state.records.isNotEmpty()) {
            item(key = "training_records") {
                if (isLoadingRecords) {
                    TrainingRecordsCardSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = loadingDescription
                                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                            },
                    )
                } else {
                    TrainingRecordsCard(
                        modifier = Modifier.fillMaxWidth(),
                        records = state.records,
                        onRecordClick = contract::onRecordClick,
                    )
                }

                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))
            }
        }

        item(key = "performance_comparison") {
            if (performance == null) {
                PerformanceComparisonCardSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = loadingDescription
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        },
                )
            } else {
                PerformanceComparisonCard(
                    modifier = Modifier.fillMaxWidth(),
                    metrics = performance.metrics,
                    baseline = performance.baseline,
                    onBaselineSelect = contract::onBaselineSelect,
                    onMetricClick = contract::onMetricClick,
                )
            }
        }

        item(key = "weekly_volume") {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            if (state.weeklyVolume == null) {
                WeeklyVolumeCardSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = loadingDescription
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        },
                )
            } else {
                WeeklyVolumeCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.weeklyVolume,
                )
            }
        }

        if (state.performance == null) {
            item(key = "training_streak_skeleton") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

                TrainingStreakCardSkeleton(
                    modifier = Modifier.fillMaxWidth(),
                    style = TrainingStreakCardStyle.Growth(previous = null),
                )
            }
        } else if (state.streak != null) {
            item(key = "training_streak") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

                TrainingStreakCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = contract::onStreakClick),
                    value = state.streak,
                    style = TrainingStreakCardStyle.Growth(previous = state.previousStreak),
                )
            }
        }

        if (state.goalProgress != null) {
            item(key = "goal_progress") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

                GoalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = contract::onGoalClick),
                    value = state.goalProgress,
                    style = GoalCardStyle.Growth(previous = state.previousGoalProgress),
                )
            }
        }

        if (state.muscleLoad != null) {
            item(key = "muscle_load") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

                MuscleLoadingCard(
                    modifier = Modifier.fillMaxWidth(),
                    summary = state.muscleLoad,
                    style = MuscleLoadingCardStyle.Session,
                )
            }
        }
    }
}

@Composable
private fun TrainingCompletedHero(
    modifier: Modifier = Modifier,
    state: TrainingCompletedState,
    training: TrainingState,
    isSharing: Boolean,
    onStatisticsClick: () -> Unit,
    onShareClick: (capture: suspend () -> ImageBitmap) -> Unit,
) {
    val style = trainingCompletedHeroStyle(state)
    val captureBackground = AppTokens.colors.background.screen
    val heroLayer = rememberGraphicsLayer()

    Column(modifier = modifier) {
        TrainingRecapHero(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    heroLayer.record {
                        drawRect(color = captureBackground)
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(heroLayer)
                },
            training = training,
            style = style,
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

        TrainingRecapActions(
            modifier = Modifier.fillMaxWidth(),
            shareEnabled = style is TrainingRecapHeroStyle.Resolved && !isSharing,
            onStatisticsClick = onStatisticsClick,
            onShareClick = {
                onShareClick { heroLayer.toImageBitmap() }
            },
        )
    }
}

@Composable
private fun trainingCompletedHeroStyle(state: TrainingCompletedState): TrainingRecapHeroStyle {
    val performance = state.performance ?: return TrainingRecapHeroStyle.Pending

    val verdict = PerformanceVerdictState.of(performance.metrics)
    val summary = verdict.summary(metrics = performance.metrics, baseline = performance.baseline)
    val rangeLabel = state.range.takeIf { it.value != null }?.label()?.text()
    val description = if (rangeLabel == null) {
        summary
    } else {
        AppTokens.strings.res(Res.string.joined_pair, summary, rangeLabel)
    }

    return TrainingRecapHeroStyle.Resolved(
        verdict = verdict,
        baseline = performance.baseline,
        description = description,
    )
}

private fun trainingCompletedPreviewState(): TrainingCompletedState {
    val createdAt = DateTimeFormatState.of(
        value = LocalDateTime(
            year = 2026,
            month = 10,
            day = 16,
            hour = 12,
            minute = 0,
        ),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    val base = stubTraining()
    val exercise = base.exercises.first()
    val iteration = stubPendingIteration().copy(
        id = "completed-set",
        repetitions = RepetitionsFormatState.of(10),
        externalWeight = VolumeFormatState.of(40f),
    )
    val exercises = List(3) { index ->
        exercise.copy(
            id = "completed-exercise-$index",
            exerciseExample = exercise.exerciseExample.copy(
                id = "completed-example-$index",
                lastUsed = createdAt,
            ),
            iterations = List(3) { setIndex ->
                iteration.copy(id = "completed-set-$index-$setIndex")
            }.toPersistentList(),
            createdAt = createdAt,
            total = exercise.total.copy(
                volume = VolumeFormatState.of(1_200f),
                intensity = IntensityFormatState.of(40f),
                repetitions = RepetitionsFormatState.of(30),
            ),
        )
    }.toPersistentList()
    val training = base.copy(
        id = "completed-training",
        exercises = exercises,
        createdAt = createdAt,
        duration = DurationFormatState.of(45.minutes),
        total = base.total.copy(
            volume = VolumeFormatState.of(3_600f),
            intensity = IntensityFormatState.of(40f),
            repetitions = RepetitionsFormatState.of(90),
        ),
    )
    val streaks = stubTrainingStreaks()

    return TrainingCompletedState(
        savedTrainingId = training.id,
        baseline = PerformanceBaselineState.Average,
        training = training,
        muscleLoad = stubMuscleLoadSummary(),
        range = DateRangeFormatState.of(DateRangeKind.Last30Days),
        performance = PerformanceComparisonState(
            metrics = stubPerformanceMetricsByType(),
            baseline = PerformanceBaselineState.Average,
        ),
        records = stubTrainingRecords(),
        weeklyVolume = stubWeeklyVolume(),
        streak = streaks[2],
        previousStreak = streaks[1],
        goalProgress = stubGoalProgress(),
        previousGoalProgress = stubGoalProgress(score = 68),
    )
}

@AppPreview
@Composable
private fun TrainingCompletedRecordPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState(),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedImprovedPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(
                performance = PerformanceComparisonState(
                    metrics = stubPerformanceMetricsImproved(),
                    baseline = PerformanceBaselineState.Average,
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedPreviousBaselinePreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(
                baseline = PerformanceBaselineState.Previous,
                performance = PerformanceComparisonState(
                    metrics = stubPerformanceMetricsImproved(),
                    baseline = PerformanceBaselineState.Previous,
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedDeclinedPreview() {
    val state = trainingCompletedPreviewState()

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                performance = PerformanceComparisonState(
                    metrics = stubPerformanceMetricsDeclined(),
                    baseline = PerformanceBaselineState.Average,
                ),
                streak = state.previousStreak,
                previousStreak = state.streak,
                goalProgress = state.previousGoalProgress,
                previousGoalProgress = state.goalProgress,
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedFirstWorkoutInPeriodPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(
                performance = PerformanceComparisonState(
                    metrics = stubPerformanceMetricsWithoutBaseline(),
                    baseline = PerformanceBaselineState.Average,
                ),
                records = persistentListOf(),
                previousStreak = null,
                goalProgress = null,
                previousGoalProgress = null,
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedRecordsPendingPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(records = persistentListOf()),
            loaders = persistentSetOf(TrainingCompletedLoader.LoadRecords),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedProgressPendingPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(
                performance = null,
                weeklyVolume = null,
                streak = null,
                previousStreak = null,
                goalProgress = null,
                previousGoalProgress = null,
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedSavingPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = TrainingCompletedState(),
            loaders = persistentSetOf(TrainingCompletedLoader.SaveTraining),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedWithoutDetailsPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = TrainingCompletedState(savedTrainingId = "saved-training"),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedResultUnavailablePreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = TrainingCompletedState(),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedLongValuesPreview() {
    val state = trainingCompletedPreviewState()
    val training = requireNotNull(state.training)

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                training = training.copy(
                    duration = DurationFormatState.of(23.hours + 59.minutes),
                    total = training.total.copy(
                        volume = VolumeFormatState.of(12_345_678f),
                        repetitions = RepetitionsFormatState.of(123_456),
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}
