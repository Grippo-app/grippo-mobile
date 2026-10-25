package com.grippo.training.completed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
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
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.OverlayContainer
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
import com.grippo.design.components.training.TrainingRecapHero
import com.grippo.design.components.training.TrainingRecapHeroState
import com.grippo.design.components.training.TrainingRecapHeroStyle
import com.grippo.design.components.training.rememberTrainingRecapHeroState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Share
import com.grippo.design.resources.provider.joined_pair
import com.grippo.design.resources.provider.loading
import com.grippo.design.resources.provider.share
import com.grippo.design.resources.provider.training_completed_details_unavailable
import com.grippo.design.resources.provider.training_completed_done
import com.grippo.design.resources.provider.training_completed_result_unavailable
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.training.completed.components.TrainingCompletedSkeleton
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
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
    val isSaving = loaders.contains(TrainingCompletedLoader.SaveTraining)
    val isSharing = loaders.contains(TrainingCompletedLoader.ShareRecap)
    val heroStyle = trainingCompletedHeroStyle(state)
    val heroLayer = rememberGraphicsLayer()
    val heroState = rememberTrainingRecapHeroState()
    val contentListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val shareState = when {
        isSaving -> ButtonState.Disabled
        isSharing -> ButtonState.Loading
        heroStyle is TrainingRecapHeroStyle.Resolved -> ButtonState.Enabled
        else -> ButtonState.Disabled
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        OverlayContainer(
            modifier = Modifier.fillMaxSize(),
            content = { resolvedPadding ->
                if (isSaving) {
                    TrainingCompletedSkeleton(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = resolvedPadding,
                    )
                } else {
                    TrainingCompletedContent(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = resolvedPadding,
                        listState = contentListState,
                        state = state,
                        loaders = loaders,
                        heroStyle = heroStyle,
                        heroState = heroState,
                        heroLayer = heroLayer,
                        contract = contract,
                    )
                }
            },
            overlay = AppTokens.colors.background.screen,
            top = {
                Spacer(modifier = Modifier.statusBarsPadding())
            },
            bottom = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            start = AppTokens.dp.screen.horizontalPadding,
                            end = AppTokens.dp.screen.horizontalPadding,
                            bottom = AppTokens.dp.screen.verticalPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.training_completed_done),
                        ),
                        style = ButtonStyle.Primary,
                        state = if (isSaving) ButtonState.Loading else ButtonState.Enabled,
                        onClick = contract::onBack,
                    )

                    if (isSaving || state.training != null) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            content = ButtonContent.Text(
                                text = AppTokens.strings.res(Res.string.share),
                                startIcon = ButtonIcon.Icon(AppTokens.icons.Share),
                            ),
                            style = ButtonStyle.Secondary,
                            state = shareState,
                            onClick = {
                                coroutineScope.launch {
                                    contentListState.animateScrollToItem(index = 0)
                                    heroState.settle()
                                    contract.onShareClick { heroLayer.toImageBitmap() }
                                }
                            },
                        )
                    }
                }
            },
        )

        KonfettiParade(
            show = state.recordCelebration == RecordCelebrationState.Playing && !isSaving,
            onShown = contract::onRecordConfettiShown,
        )
    }
}

@Composable
private fun TrainingCompletedContent(
    modifier: Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState,
    state: TrainingCompletedState,
    loaders: ImmutableSet<TrainingCompletedLoader>,
    heroStyle: TrainingRecapHeroStyle,
    heroState: TrainingRecapHeroState,
    heroLayer: GraphicsLayer,
    contract: TrainingCompletedContract,
) {
    val training = state.training
    val performance = state.performance
    val isLoadingRecords = loaders.contains(TrainingCompletedLoader.LoadRecords)
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
    ) {
        if (training == null) {
            item(key = "completion_unavailable") {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                        .semantics { heading() },
                    text = AppTokens.strings.res(
                        if (state.savedTrainingId != null) {
                            Res.string.training_completed_details_unavailable
                        } else {
                            Res.string.training_completed_result_unavailable
                        },
                    ),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
            return@LazyColumn
        }

        item(key = "completion_hero") {
            TrainingCompletedHero(
                modifier = Modifier.fillMaxWidth(),
                training = training,
                style = heroStyle,
                state = heroState,
                layer = heroLayer,
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))
        }

        if (isLoadingRecords || state.records.isNotEmpty()) {
            item(key = "training_records") {
                if (isLoadingRecords) {
                    TrainingRecordsCardSkeleton(
                        modifier = cardModifier.semantics(mergeDescendants = true) {
                            contentDescription = loadingDescription
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        },
                    )
                } else {
                    TrainingRecordsCard(
                        modifier = cardModifier,
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
                    modifier = cardModifier.semantics(mergeDescendants = true) {
                        contentDescription = loadingDescription
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    },
                )
            } else {
                PerformanceComparisonCard(
                    modifier = cardModifier,
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
                    modifier = cardModifier.semantics(mergeDescendants = true) {
                        contentDescription = loadingDescription
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    },
                )
            } else {
                WeeklyVolumeCard(
                    modifier = cardModifier,
                    value = state.weeklyVolume,
                )
            }
        }

        if (performance == null) {
            item(key = "training_streak_skeleton") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

                TrainingStreakCardSkeleton(
                    modifier = cardModifier,
                    style = TrainingStreakCardStyle.Growth(previous = null),
                )
            }
        } else if (state.streak != null) {
            item(key = "training_streak") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

                TrainingStreakCard(
                    modifier = cardModifier.scalableClick(onClick = contract::onStreakClick),
                    value = state.streak,
                    style = TrainingStreakCardStyle.Growth(previous = state.previousStreak),
                )
            }
        }

        if (state.goalProgress != null) {
            item(key = "goal_progress") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

                GoalCard(
                    modifier = cardModifier.scalableClick(onClick = contract::onGoalClick),
                    value = state.goalProgress,
                    style = GoalCardStyle.Growth(previous = state.previousGoalProgress),
                )
            }
        }

        if (state.muscleLoad != null) {
            item(key = "muscle_load") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

                MuscleLoadingCard(
                    modifier = cardModifier,
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
    training: TrainingState,
    style: TrainingRecapHeroStyle,
    state: TrainingRecapHeroState,
    layer: GraphicsLayer,
) {
    val captureBackground = AppTokens.colors.background.screen

    TrainingRecapHero(
        modifier = modifier.drawWithContent {
            layer.record {
                drawRect(color = captureBackground)
                this@drawWithContent.drawContent()
            }
            drawLayer(layer)
        },
        training = training,
        style = style,
        state = state,
    )
}

@Composable
private fun trainingCompletedHeroStyle(state: TrainingCompletedState): TrainingRecapHeroStyle {
    val performance = state.performance ?: return TrainingRecapHeroStyle.Pending

    val verdict = PerformanceVerdictState.of(performance.metrics)
    val summary = verdict.summary(metrics = performance.metrics, baseline = performance.baseline)
    val rangeLabel = if (performance.isOver(state.range.value)) {
        state.range.label()?.text()
    } else {
        performance.period.short()
    }
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

private fun previewRhythmStreak(length: Int): TrainingStreakState {
    val streak = stubTrainingStreaks().first { it.featured is TrainingStreakFeaturedState.Rhythm }
    val featured = streak.featured as TrainingStreakFeaturedState.Rhythm

    return streak.copy(featured = featured.copy(length = length))
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
    val template = base.exercises.first()
    val iteration = stubPendingIteration().copy(
        id = "completed-set",
        repetitions = RepetitionsFormatState.of(10),
        externalWeight = VolumeFormatState.of(40f),
    )
    val total = base.total.copy(
        volume = VolumeFormatState.ofTotal(1_200f),
        intensity = IntensityFormatState.ofAverage(40f),
        repetitions = RepetitionsFormatState.ofTotal(30),
    )
    val exercise = template.copy(
        id = "completed-exercise",
        exerciseExample = template.exerciseExample.copy(
            id = "completed-example",
            lastUsed = createdAt,
        ),
        iterations = List(3) { setIndex ->
            iteration.copy(id = "completed-set-$setIndex")
        }.toPersistentList(),
        createdAt = createdAt,
        total = total,
    )
    val training = base.copy(
        id = "completed-training",
        exercises = persistentListOf(exercise),
        createdAt = createdAt,
        duration = DurationFormatState.ofTotal(65.minutes),
        total = total,
    )

    val range = DateRangeFormatState.of(DateRangeKind.Last30Days)

    return TrainingCompletedState(
        savedTrainingId = training.id,
        baseline = PerformanceBaselineState.Average,
        training = training,
        muscleLoad = stubMuscleLoadSummary(),
        range = range,
        performance = PerformanceComparisonState(
            metrics = stubPerformanceMetricsByType(),
            baseline = PerformanceBaselineState.Average,
            period = range,
        ),
        records = stubTrainingRecords(),
        weeklyVolume = stubWeeklyVolume(),
        streak = previewRhythmStreak(length = 3),
        previousStreak = previewRhythmStreak(length = 2),
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
    val state = trainingCompletedPreviewState()

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                performance = state.performance?.copy(metrics = stubPerformanceMetricsImproved()),
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedPreviousBaselinePreview() {
    val state = trainingCompletedPreviewState()

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                baseline = PerformanceBaselineState.Previous,
                performance = state.performance?.copy(
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
    val training = requireNotNull(state.training)
    val metrics = stubPerformanceMetricsDeclined()
    val volume = metrics.filterIsInstance<PerformanceMetricState.Volume>().first().current

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                training = training.copy(
                    total = training.total.copy(volume = VolumeFormatState.ofTotal(volume)),
                ),
                performance = state.performance?.copy(metrics = metrics),
                streak = previewRhythmStreak(length = 1),
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
    val state = trainingCompletedPreviewState()

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                performance = state.performance?.copy(metrics = stubPerformanceMetricsWithoutBaseline()),
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
private fun TrainingCompletedOlderWorkoutPreview() {
    val state = trainingCompletedPreviewState()

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                performance = state.performance?.copy(
                    metrics = stubPerformanceMetricsImproved(),
                    period = DateRangeFormatState.of(
                        kind = DateRangeKind.Last30Days,
                        anchor = LocalDate(year = 2026, month = 8, day = 11),
                    ),
                ),
                previousStreak = null,
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
                    duration = DurationFormatState.ofTotal(23.hours + 59.minutes),
                    total = training.total.copy(
                        volume = VolumeFormatState.ofTotal(12_345_678f),
                        repetitions = RepetitionsFormatState.ofTotal(123_456),
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}
