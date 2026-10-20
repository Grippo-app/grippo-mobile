package com.grippo.training.streak.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.engagement.TrainingStreakMood
import com.grippo.design.components.datetime.DialogRangeHeader
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCard
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakInsightsCard
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakTimelineCard
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyListState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_streak_insights_title
import com.grippo.design.resources.provider.training_streak_timeline_title
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun TrainingStreakDetailsScreen(
    state: TrainingStreakDetailsDialogState,
    loaders: ImmutableSet<TrainingStreakDetailsLoader>,
    contract: TrainingStreakDetailsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    DialogRangeHeader(text = state.range.short())

    val listState = rememberAnchoredLazyListState(
        behavior = AnchorScrollBehavior.Animated
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = AppTokens.dp.dialog.horizontalPadding,
            end = AppTokens.dp.dialog.horizontalPadding,
            bottom = AppTokens.dp.dialog.bottom,
        ),
    ) {
        state.streak?.let { streak ->
            item(key = "summary") {
                TrainingStreakCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = streak
                )
            }

            item(key = "insights_header") {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTokens.dp.contentPadding.block),
                    text = AppTokens.strings.res(Res.string.training_streak_insights_title),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }

            item(key = "insights") {
                TrainingStreakInsightsCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTokens.dp.contentPadding.content),
                    value = streak
                )
            }

            if (streak.timeline.isNotEmpty()) {
                item(key = "timeline_header") {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTokens.dp.contentPadding.block),
                        text = AppTokens.strings.res(Res.string.training_streak_timeline_title),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                }

                item(key = "timeline") {
                    TrainingStreakTimelineCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTokens.dp.contentPadding.content),
                        entries = streak.timeline
                    )
                }
            }
        }

    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = TrainingStreakDetailsDialogState(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(2026, 10, 11, 0, 0),
                    to = LocalDateTime(2026, 10, 17, 23, 59),
                ),
                streak = stubTrainingStreaks().first()
            ),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun NoActiveDaysPreview() {
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
            state = TrainingStreakDetailsDialogState(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(2026, 10, 11, 0, 0),
                    to = LocalDateTime(2026, 10, 17, 23, 59),
                ),
                streak = streak,
            ),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun WithoutStreakPreview() {
    PreviewContainer {
        TrainingStreakDetailsScreen(
            state = TrainingStreakDetailsDialogState(
                range = DateRangeFormatState.of(
                    from = LocalDateTime(2026, 10, 11, 0, 0),
                    to = LocalDateTime(2026, 10, 17, 23, 59),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingStreakDetailsContract.Empty,
        )
    }
}
