package com.grippo.training.goal.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalFitDriftingCard
import com.grippo.design.components.metrics.profile.goal.GoalFitOnTrackCard
import com.grippo.design.components.metrics.profile.goal.GoalInsightCard
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyListState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_details_analysis_evidence
import com.grippo.design.resources.provider.goal_details_analysis_excluded
import com.grippo.design.resources.provider.goal_details_analysis_method
import com.grippo.design.resources.provider.goal_details_analysis_missing_metadata
import com.grippo.design.resources.provider.goal_details_analysis_period
import com.grippo.design.resources.provider.goal_details_analysis_unavailable
import com.grippo.design.resources.provider.goal_details_insights_title
import com.grippo.design.resources.provider.goal_details_subtitle
import com.grippo.design.resources.provider.goal_fit_diagnostic_drifting_title
import com.grippo.design.resources.provider.goal_fit_diagnostic_on_track_title
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun TrainingGoalDetailsScreen(
    state: TrainingGoalDetailsDialogState,
    loaders: ImmutableSet<TrainingGoalDetailsLoader>,
    contract: TrainingGoalDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        text = AppTokens.strings.res(Res.string.goal_details_subtitle),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    ContentDivider()

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
            top = AppTokens.dp.contentPadding.content,
            bottom = AppTokens.dp.dialog.bottom,
        ),
    ) {
        val progress = state.progress
        val insights = state.insights

        if (progress != null) {
            item(key = "hero") {
                GoalCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = progress,
                    onUpdateClick = contract::onAddGoal
                )
            }

            item(key = "description") {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTokens.dp.contentPadding.content),
                    text = progress.goal.primaryGoal.description(),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }

            item(key = "evidence") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTokens.dp.contentPadding.content),
                ) {
                    val analysisRange = progress.evidence.range.value
                    val periodText = if (analysisRange != null) {
                        val from = DateCompose.rememberFormat(
                            value = analysisRange.from,
                            format = DateFormat.DateOnly.DateMmmDdYyyy,
                        )
                        val to = DateCompose.rememberFormat(
                            value = analysisRange.to,
                            format = DateFormat.DateOnly.DateMmmDdYyyy,
                        )
                        AppTokens.strings.res(Res.string.goal_details_analysis_period, from, to)
                    } else {
                        AppTokens.strings.res(Res.string.goal_details_analysis_unavailable)
                    }

                    Text(
                        text = periodText,
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.primary,
                    )

                    Text(
                        text = AppTokens.strings.res(
                            Res.string.goal_details_analysis_evidence,
                            progress.evidence.days,
                            progress.evidence.setCount,
                        ),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.secondary,
                    )

                    if (progress.evidence.excludedSetCount > 0) {
                        Text(
                            text = AppTokens.strings.res(
                                Res.string.goal_details_analysis_excluded,
                                progress.evidence.excludedSetCount,
                            ),
                            style = AppTokens.typography.b13Med(),
                            color = AppTokens.colors.text.secondary,
                        )
                    }

                    if (progress.evidence.missingMetadataSetCount > 0) {
                        Text(
                            text = AppTokens.strings.res(
                                Res.string.goal_details_analysis_missing_metadata,
                                progress.evidence.missingMetadataSetCount,
                            ),
                            style = AppTokens.typography.b13Med(),
                            color = AppTokens.colors.text.secondary,
                        )
                    }

                    Text(
                        modifier = Modifier.padding(top = AppTokens.dp.contentPadding.content),
                        text = AppTokens.strings.res(Res.string.goal_details_analysis_method),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }

            if (progress.findings.any { it.severity != GoalFitSeverityState.PASS }) {
                item(key = "drifting_header") {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTokens.dp.contentPadding.block),
                        text = AppTokens.strings.res(Res.string.goal_fit_diagnostic_drifting_title),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                }

                item(key = "drifting") {
                    GoalFitDriftingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTokens.dp.contentPadding.content),
                        findings = progress.findings,
                    )
                }
            }

            if (progress.findings.any { it.severity == GoalFitSeverityState.PASS }) {
                item(key = "on_track_header") {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTokens.dp.contentPadding.block),
                        text = AppTokens.strings.res(Res.string.goal_fit_diagnostic_on_track_title),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                }

                item(key = "on_track") {
                    GoalFitOnTrackCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppTokens.dp.contentPadding.content),
                        findings = progress.findings,
                    )
                }
            }
        }

        item(key = "insights_header") {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (progress != null) AppTokens.dp.contentPadding.block else 0.dp),
                text = AppTokens.strings.res(Res.string.goal_details_insights_title),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )
        }

        items(
            items = insights,
            key = { it.reason },
        ) { item ->
            val (headline, detail) = item.reason.reasonText()
            val action = item.action?.tipText()
            GoalInsightCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTokens.dp.contentPadding.content),
                severity = item.severity.goalInsightSeverity(),
                headline = headline,
                detail = detail,
                action = action,
            )
        }

    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenPreview() {
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = previewGoalProgress(score = 54),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun InsightsWithoutProgressPreview() {
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenOnTrackPreview() {
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = previewGoalProgress(score = 82),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenTooEarlyPreview() {
    val base = previewGoalProgress(score = null)
    val progress = base.copy(
        evidence = base.evidence.copy(
            range = DateRangeFormatState.of(
                from = LocalDateTime(2026, 10, 16, 0, 0),
                to = LocalDateTime(2026, 10, 17, 23, 59),
            ),
            setCount = 12,
        ),
        findings = persistentListOf(),
    )
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = progress,
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenEmptyPeriodPreview() {
    val base = previewGoalProgress(score = null)
    val progress = base.copy(
        sessionCount = 0,
        findings = persistentListOf(),
        evidence = base.evidence.copy(setCount = 0),
    )
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = progress,
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenPartialEvidencePreview() {
    val base = previewGoalProgress(primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE, score = 50)
    val progress = base.copy(
        evidence = base.evidence.copy(
            setCount = 24,
            excludedSetCount = 3,
            missingMetadataSetCount = 8,
        ),
    )
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = progress,
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}


@AppPreview
@Composable
private fun TrainingGoalDetailsScreenNoOverlapPreview() {
    val base = previewGoalProgress(score = null)
    val progress = base.copy(
        evidence = base.evidence.copy(
            range = DateRangeFormatState.Empty(),
            setCount = 0,
        ),
        sessionCount = 0,
        findings = persistentListOf(),
    )
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = progress,
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

private fun previewGoalProgress(
    score: Int?,
    primary: GoalPrimaryGoalEnumState = GoalPrimaryGoalEnumState.GET_STRONGER,
): GoalProgressState {
    val progress = stubGoalProgress(primary = primary, score = score)
    val now = DateTimeFormatState.of(
        value = LocalDateTime(2026, 10, 18, 12, 0),
        range = DateTimeUtils.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    val createdAt = DateTimeFormatState.of(
        value = LocalDateTime(2026, 9, 30, 12, 0),
        range = DateTimeUtils.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    val target = DateTimeFormatState.of(
        value = LocalDateTime(2026, 12, 29, 12, 0),
        range = DateTimeUtils.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    return progress.copy(
        goal = progress.goal.copy(
            createdAt = createdAt,
            target = target,
            personalizations = persistentListOf(),
        ),
        now = now,
        evidence = progress.evidence.copy(range = previewAnalysisRange()),
    )
}

private fun previewAnalysisRange(): DateRangeFormatState = DateRangeFormatState.of(
    from = LocalDateTime(2026, 10, 11, 0, 0),
    to = LocalDateTime(2026, 10, 17, 23, 59),
)
