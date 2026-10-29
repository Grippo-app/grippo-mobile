package com.grippo.training.streak.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.engagement.TrainingStreakMood
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.engagement.stubTrainingStreakProgress
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.datetime.DialogRangeHeader
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCard
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardStyle
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakTimelineCard
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyListState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.design.resources.provider.icons.ArrowTop
import com.grippo.design.resources.provider.training_streak_details_empty
import com.grippo.design.resources.provider.training_streak_details_method_collapsed
import com.grippo.design.resources.provider.training_streak_details_method_expanded
import com.grippo.design.resources.provider.training_streak_insights_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

private const val MethodAnimationDurationMillis = 200

@Composable
internal fun TrainingStreakDetailsScreen(
    state: TrainingStreakDetailsDialogState,
    loaders: ImmutableSet<TrainingStreakDetailsLoader>,
    contract: TrainingStreakDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    var isMethodExpanded by rememberSaveable(state.range) { mutableStateOf(false) }

    DialogRangeHeader(text = state.range.shortStartEnd())

    TrainingStreakDetailsContent(
        modifier = Modifier
            .fillMaxWidth()
            .weight(weight = 1f, fill = false),
        state = state,
        isMethodExpanded = isMethodExpanded,
        onToggleMethod = { isMethodExpanded = !isMethodExpanded },
    )
}

@Composable
private fun TrainingStreakDetailsContent(
    modifier: Modifier = Modifier,
    state: TrainingStreakDetailsDialogState,
    isMethodExpanded: Boolean,
    onToggleMethod: () -> Unit,
) {
    val listState = rememberAnchoredLazyListState(
        behavior = AnchorScrollBehavior.Animated,
    )
    val streak = state.streak

    OverlayContainer(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        overlay = AppTokens.colors.background.dialog,
        content = { resolvedPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = resolvedPadding,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
            ) {
                when {
                    streak == null -> Unit

                    streak.totalActiveDays == 0 -> {
                        item(key = "empty") {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = AppTokens.strings.res(Res.string.training_streak_details_empty),
                                style = AppTokens.typography.b14Med(),
                                color = AppTokens.colors.text.secondary,
                            )
                        }
                    }

                    else -> {
                        item(key = "summary") {
                            TrainingStreakCard(
                                modifier = Modifier.fillMaxWidth(),
                                value = streak,
                                style = TrainingStreakCardStyle.Detailed,
                            )
                        }

                        if (streak.timeline.isNotEmpty()) {
                            item(key = "timeline") {
                                TrainingStreakTimelineCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = streak,
                                )
                            }
                        }

                        item(key = "method") {
                            TrainingStreakMethodDisclosure(
                                modifier = Modifier.fillMaxWidth(),
                                value = streak,
                                isExpanded = isMethodExpanded,
                                onToggle = onToggleMethod,
                            )
                        }
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@Composable
private fun TrainingStreakMethodDisclosure(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val expansionDescription = AppTokens.strings.res(
        if (isExpanded) Res.string.training_streak_details_method_expanded
        else Res.string.training_streak_details_method_collapsed,
    )

    Column(modifier = modifier) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = expansionDescription },
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.training_streak_insights_title),
                endIcon = ButtonIcon.Icon(
                    value = if (isExpanded) AppTokens.icons.ArrowTop else AppTokens.icons.ArrowDown,
                ),
            ),
            style = ButtonStyle.Tertiary,
            onClick = onToggle,
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(MethodAnimationDurationMillis),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(MethodAnimationDurationMillis)),
            exit = shrinkVertically(
                animationSpec = tween(MethodAnimationDurationMillis),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(MethodAnimationDurationMillis)),
        ) {
            Text(
                modifier = Modifier.padding(top = AppTokens.dp.contentPadding.content),
                text = value.featured.calculationDescription(),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingStreakDailyPreview() {
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = trainingStreakDetailsPreview(streak = dailyStreakPreview()),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakWeeklyPreview() {
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = trainingStreakDetailsPreview(streak = weeklyStreakPreview()),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakRhythmPreview() {
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = trainingStreakDetailsPreview(streak = stubTrainingStreaks()[2]),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakPatternPreview() {
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = trainingStreakDetailsPreview(streak = patternStreakPreview()),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakActiveHistoryWithoutCurrentRunPreview() {
    val daily = dailyStreakPreview()
    val streak = daily.copy(
        featured = TrainingStreakFeaturedState.Daily(
            length = 0,
            mood = TrainingStreakMood.Paused,
            progressPercent = 0,
            confidence = 0.2f,
        ),
        timeline = daily.timeline.mapIndexed { index, entry ->
            entry.copy(achievedTrainingDays = if (index >= 5) 0 else entry.achievedTrainingDays)
        }.toPersistentList(),
        lastTrainingGapDays = 2,
    )
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = trainingStreakDetailsPreview(streak = streak),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakPartialLeadingPeriodPreview() {
    val weekly = stubTrainingStreaks()[1]
    val streak = weekly.copy(
        totalActiveDays = 14,
        historyDays = 25,
        lastTrainingGapDays = 1,
        featured = (weekly.featured as TrainingStreakFeaturedState.Weekly).copy(
            length = 3,
            progressPercent = 100,
        ),
        timeline = persistentListOf(
            stubTrainingStreakProgress().copy(
                achievedTrainingDays = 2,
                isPartial = true,
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 8, dayOfMonth = 31, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 6, hour = 23, minute = 59),
                ),
            ),
            stubTrainingStreakProgress().copy(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 7, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 13, hour = 23, minute = 59),
                ),
            ),
            stubTrainingStreakProgress().copy(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 14, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 20, hour = 23, minute = 59),
                ),
            ),
            stubTrainingStreakProgress().copy(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 21, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 27, hour = 23, minute = 59),
                ),
            ),
        ),
    )
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = trainingStreakDetailsPreview(streak = streak).copy(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 3, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 27, hour = 23, minute = 59),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakWithoutTimelinePreview() {
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = trainingStreakDetailsPreview(streak = dailyStreakPreview().copy(timeline = persistentListOf())),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakNoActiveDaysPreview() {
    val streak = stubTrainingStreaks().first().copy(
        totalActiveDays = 0,
        featured = TrainingStreakFeaturedState.Daily(
            length = 0,
            mood = TrainingStreakMood.Restart,
            progressPercent = 0,
            confidence = 0f,
        ),
        timeline = persistentListOf(),
        historyDays = 0,
        lastTrainingGapDays = -1,
    )
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = trainingStreakDetailsPreview(streak = streak),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakExpandedMethodPreview() {
    PreviewContainer {
        val state = trainingStreakDetailsPreview(streak = dailyStreakPreview())
        BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
            DialogRangeHeader(text = state.range.shortStartEnd())
            TrainingStreakDetailsContent(
                modifier = Modifier.fillMaxWidth().weight(1f, false),
                state = state,
                isMethodExpanded = true,
                onToggleMethod = {},
            )
        }
    }
}

private fun trainingStreakDetailsPreview(streak: TrainingStreakState?): TrainingStreakDetailsDialogState {
    return TrainingStreakDetailsDialogState(
        range = DateRangeFormatState.of(
            from = LocalDateTime(year = 2026, monthNumber = 8, dayOfMonth = 26, hour = 0, minute = 0),
            to = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 20, hour = 23, minute = 59),
        ),
        streak = streak,
    )
}

private fun dailyStreakPreview(): TrainingStreakState {
    return stubTrainingStreaks().first().copy(
        timeline = (14..20).map { day ->
            stubTrainingStreakProgress().copy(
                achievedTrainingDays = if (day >= 16) 1 else 0,
                targetTrainingDays = 1,
                isPartial = day == 20,
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = day, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = day, hour = 23, minute = 59),
                ),
            )
        }.toPersistentList(),
    )
}

private fun weeklyStreakPreview(): TrainingStreakState {
    val weekly = stubTrainingStreaks()[1]
    val previous = stubTrainingStreakProgress().copy(
        achievedTrainingDays = 0,
        range = DateRangeFormatState.of(
            from = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 28, hour = 0, minute = 0),
            to = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 4, hour = 23, minute = 59),
        ),
    )
    return weekly.copy(
        historyDays = 30,
        timeline = (persistentListOf(previous) + weekly.timeline).toPersistentList(),
    )
}

private fun patternStreakPreview(): TrainingStreakState {
    val streak = stubTrainingStreaks()[3]
    val featured = streak.featured as TrainingStreakFeaturedState.Pattern
    return streak.copy(
        totalActiveDays = 21,
        featured = featured.copy(length = 4, progressPercent = 100),
        historyDays = 56,
        lastTrainingGapDays = 3,
        timeline = persistentListOf(
            stubTrainingStreakProgress().copy(
                achievedTrainingDays = 3,
                targetTrainingDays = 3,
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 19, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 26, hour = 23, minute = 59),
                ),
            ),
            stubTrainingStreakProgress().copy(
                achievedTrainingDays = 3,
                targetTrainingDays = 3,
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 9, dayOfMonth = 27, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 4, hour = 23, minute = 59),
                ),
            ),
            stubTrainingStreakProgress().copy(
                achievedTrainingDays = 3,
                targetTrainingDays = 3,
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 5, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 12, hour = 23, minute = 59),
                ),
            ),
            stubTrainingStreakProgress().copy(
                achievedTrainingDays = 3,
                targetTrainingDays = 3,
                isPartial = true,
                range = DateRangeFormatState.of(
                    from = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 13, hour = 0, minute = 0),
                    to = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 20, hour = 23, minute = 59),
                ),
            ),
        ),
    )
}
