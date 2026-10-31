package com.grippo.training.completed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceVerdictState
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsByType
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsDeclined
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsImproved
import com.grippo.core.state.metrics.performance.stubPerformanceMetricsWithoutReference
import com.grippo.core.state.metrics.performance.stubTrainingRecords
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.core.state.metrics.volume.stubWeeklyVolume
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubRecapTraining
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
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardStyle
import com.grippo.design.components.metrics.performance.PerformanceComparisonSection
import com.grippo.design.components.metrics.performance.TrainingRecordsCard
import com.grippo.design.components.metrics.performance.TrainingRecordsCardSkeleton
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalCardStyle
import com.grippo.design.components.metrics.volume.WeeklyVolumeSection
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.training.TrainingRecapHero
import com.grippo.design.components.training.TrainingRecapHeroSkeleton
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
import com.grippo.design.resources.provider.progress
import com.grippo.design.resources.provider.share
import com.grippo.design.resources.provider.training_completed_details_unavailable
import com.grippo.design.resources.provider.training_completed_done
import com.grippo.design.resources.provider.training_completed_result_unavailable
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.training.completed.components.ProgressDivider
import com.grippo.training.completed.components.TrainingCompletedSkeleton
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
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
    val training = state.training
    val performance = state.performance
    val heroStyle = performance?.let { trainingCompletedHeroStyle(performance = it, range = state.range) }
    val heroLayer = rememberGraphicsLayer()
    val heroState = rememberTrainingRecapHeroState()
    val heroPadding = WindowInsets.statusBars.asPaddingValues()
    val contentListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val shareState = when {
        isSaving -> ButtonState.Disabled
        isSharing -> ButtonState.Loading
        heroStyle != null -> ButtonState.Enabled
        else -> ButtonState.Disabled
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                isSaving -> TrainingRecapHeroSkeleton(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = heroPadding,
                )

                training != null && heroStyle != null -> TrainingCompletedHero(
                    modifier = Modifier.fillMaxWidth(),
                    training = training,
                    style = heroStyle,
                    state = heroState,
                    layer = heroLayer,
                    contentPadding = heroPadding,
                )

                else -> Spacer(modifier = Modifier.statusBarsPadding())
            }

            OverlayContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(heroState.nestedScrollConnection),
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
                            isLoadingRecords = TrainingCompletedLoader.LoadRecords in loaders,
                            contract = contract,
                        )
                    }
                },
                overlay = AppTokens.colors.background.screen,
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

                        if (isSaving || training != null) {
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
                                        val scrollToTop = async { contentListState.animateScrollToItem(index = 0) }
                                        heroState.whileExpanded {
                                            scrollToTop.await()
                                            heroState.settle()
                                            contract.onShareClick { heroLayer.toImageBitmap() }
                                        }
                                    }
                                },
                            )
                        }
                    }
                },
            )
        }

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
    isLoadingRecords: Boolean,
    contract: TrainingCompletedContract,
) {
    val performance = state.performance
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val sectionModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTokens.dp.screen.horizontalPadding)

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.block,
            bottom = contentPadding.calculateBottomPadding(),
        ),
    ) {
        if (state.training == null) {
            item(key = "completion_unavailable") {
                Text(
                    modifier = sectionModifier.semantics { heading() },
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

        item(key = "performance_comparison") {
            Column(modifier = Modifier.fillMaxWidth()) {
                AnimatedContent(
                    modifier = Modifier.fillMaxWidth(),
                    targetState = if (isLoadingRecords) null else state.records,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 150)))
                            .using(
                                SizeTransform(sizeAnimationSpec = { _, _ ->
                                    tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                }),
                            )
                    },
                    contentAlignment = Alignment.TopStart,
                    label = "TrainingCompletedRecords",
                ) { records ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (records == null) {
                            TrainingRecordsCardSkeleton(
                                modifier = sectionModifier.semantics(mergeDescendants = true) {
                                    contentDescription = loadingDescription
                                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                                },
                            )
                        } else if (records.isNotEmpty()) {
                            TrainingRecordsCard(
                                modifier = sectionModifier,
                                records = records,
                                onRecordClick = contract::onRecordClick,
                            )
                        }

                        if (records == null || records.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))
                        }
                    }
                }

                if (performance != null) {
                    PerformanceComparisonSection(
                        modifier = sectionModifier,
                        metrics = performance.metrics,
                        reference = performance.reference,
                        onReferenceSelect = contract::onReferenceSelect,
                        onMetricClick = contract::onMetricClick,
                    )
                }
            }
        }

        if (state.weeklyVolume != null || state.streak != null || state.goalProgress != null) {
            item(key = "progress_heading") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

                Text(
                    modifier = sectionModifier.semantics { heading() },
                    text = AppTokens.strings.res(Res.string.progress),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )

                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))
            }
        }

        if (state.weeklyVolume != null) {
            item(key = "weekly_volume") {
                WeeklyVolumeSection(
                    modifier = sectionModifier,
                    value = state.weeklyVolume,
                )
            }
        }

        if (state.streak != null) {
            item(key = "training_streak") {
                if (state.weeklyVolume != null) {
                    ProgressDivider(modifier = sectionModifier)
                }

                TrainingStreakCard(
                    modifier = sectionModifier.scalableClick(onClick = contract::onStreakClick),
                    value = state.streak,
                    style = TrainingStreakCardStyle.Growth(previous = state.previousStreak),
                )
            }
        }

        if (state.goalProgress != null) {
            item(key = "goal_progress") {
                if (state.weeklyVolume != null || state.streak != null) {
                    ProgressDivider(modifier = sectionModifier)
                }

                GoalCard(
                    modifier = sectionModifier.scalableClick(onClick = contract::onGoalClick),
                    value = state.goalProgress,
                    style = GoalCardStyle.Growth(previous = state.previousGoalProgress),
                )
            }
        }

        if (state.muscleLoad != null) {
            item(key = "muscle_load") {
                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

                MuscleLoadingCard(
                    modifier = sectionModifier,
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
    contentPadding: PaddingValues,
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
        contentPadding = contentPadding,
    )
}

@Composable
private fun trainingCompletedHeroStyle(
    performance: PerformanceComparisonState,
    range: DateRangeFormatState,
): TrainingRecapHeroStyle.Resolved {
    val verdict = PerformanceVerdictState.of(performance.metrics)
    val summary = verdict.summary(metrics = performance.metrics, reference = performance.reference)
    val rangeLabel = performance.periodLabel(range)
    val description = if (rangeLabel == null) {
        summary
    } else {
        AppTokens.strings.res(Res.string.joined_pair, summary, rangeLabel)
    }

    return TrainingRecapHeroStyle.Resolved(
        verdict = verdict,
        reference = performance.reference,
        description = description,
    )
}

private fun previewRhythmStreak(length: Int): TrainingStreakState {
    val streak = stubTrainingStreaks().first { it.featured is TrainingStreakFeaturedState.Rhythm }
    val featured = streak.featured as TrainingStreakFeaturedState.Rhythm

    return streak.copy(featured = featured.copy(length = length))
}

private fun trainingCompletedPreviewState(): TrainingCompletedState {
    val training = stubRecapTraining()
    val range = DateRangeFormatState.of(DateRangeKind.Last30Days)

    return TrainingCompletedState(
        savedTrainingId = training.id,
        reference = PerformanceReferenceState.Average,
        training = training,
        muscleLoad = stubMuscleLoadSummary(),
        range = range,
        performance = PerformanceComparisonState(
            metrics = stubPerformanceMetricsByType(),
            reference = PerformanceReferenceState.Average,
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
private fun TrainingCompletedPreviousReferencePreview() {
    val state = trainingCompletedPreviewState()

    PreviewContainer {
        TrainingCompletedScreen(
            state = state.copy(
                reference = PerformanceReferenceState.Previous,
                performance = state.performance?.copy(
                    metrics = stubPerformanceMetricsImproved(),
                    reference = PerformanceReferenceState.Previous,
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
                performance = state.performance?.copy(metrics = stubPerformanceMetricsWithoutReference()),
                records = persistentListOf(),
                streak = previewRhythmStreak(length = 1),
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
                        anchor = LocalDate(year = 2026, month = 5, day = 27),
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
private fun TrainingCompletedStreakOnlyProgressPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(
                weeklyVolume = null,
                goalProgress = null,
                previousGoalProgress = null,
                muscleLoad = null,
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedGoalOnlyProgressPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = trainingCompletedPreviewState().copy(
                weeklyVolume = null,
                streak = null,
                previousStreak = null,
                muscleLoad = null,
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingCompletedOptionalResultsUnavailablePreview() {
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
