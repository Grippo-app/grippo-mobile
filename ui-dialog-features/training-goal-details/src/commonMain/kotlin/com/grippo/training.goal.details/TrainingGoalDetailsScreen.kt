package com.grippo.training.goal.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.grippo.core.state.metrics.profile.GoalAnalysisEvidenceState
import com.grippo.core.state.metrics.profile.GoalFitFindingState
import com.grippo.core.state.metrics.profile.GoalFitRuleState
import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalFitDriftingCard
import com.grippo.design.components.metrics.profile.goal.GoalFitOnTrackCard
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
import com.grippo.design.resources.provider.goal_details_hide_details
import com.grippo.design.resources.provider.goal_details_show_details
import com.grippo.design.resources.provider.goal_fit_diagnostic_drifting_title
import com.grippo.design.resources.provider.goal_fit_diagnostic_on_track_title
import com.grippo.design.resources.provider.goal_options_collapsed
import com.grippo.design.resources.provider.goal_options_expanded
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.design.resources.provider.icons.ArrowTop
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

private const val DisclosureAnimationDurationMillis = 200

@Composable
internal fun TrainingGoalDetailsScreen(
    state: TrainingGoalDetailsDialogState,
    loaders: ImmutableSet<TrainingGoalDetailsLoader>,
    contract: TrainingGoalDetailsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    val goal = state.progress?.goal
    var isExpanded by rememberSaveable(goal?.primaryGoal, goal?.createdAt?.value) {
        mutableStateOf(false)
    }

    TrainingGoalDetailsContent(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
        state = state,
        isExpanded = isExpanded,
        onToggleDetails = { isExpanded = !isExpanded },
        onAddGoal = contract::onAddGoal,
    )
}

@Composable
private fun TrainingGoalDetailsContent(
    modifier: Modifier = Modifier,
    state: TrainingGoalDetailsDialogState,
    isExpanded: Boolean,
    onToggleDetails: () -> Unit,
    onAddGoal: () -> Unit,
) {
    val progress = state.progress
    val priorityFindings = state.priorityFindings
    val notice = state.notice()
    val listState = rememberAnchoredLazyListState(behavior = AnchorScrollBehavior.Animated)

    LazyColumn(
        state = listState,
        modifier = modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = AppTokens.dp.dialog.horizontalPadding,
            end = AppTokens.dp.dialog.horizontalPadding,
            top = AppTokens.dp.dialog.top,
            bottom = AppTokens.dp.dialog.bottom,
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        if (progress != null) {
            item(key = "hero") {
                GoalCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = progress,
                    presentation = MetricCardPresentation.Detailed,
                    onUpdateClick = onAddGoal,
                )
            }

            if (progress.evidence.range.value != null) {
                item(key = "sample") {
                    GoalAnalysisSummary(
                        modifier = Modifier.fillMaxWidth(),
                        value = progress.evidence,
                    )
                }
            }
        }

        if (notice != null) {
            item(key = "notice") {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = notice,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        if (priorityFindings.isNotEmpty()) {
            item(key = "priority_findings") {
                GoalAdjustmentSection(
                    modifier = Modifier.fillMaxWidth(),
                    findings = priorityFindings,
                )
            }
        }

        if (progress != null) {
            item(key = "disclosure") {
                GoalAssessmentDisclosure(
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                    isExpanded = isExpanded,
                    onToggleDetails = onToggleDetails,
                )
            }
        }
    }
}

@Composable
private fun GoalAnalysisSummary(
    modifier: Modifier = Modifier,
    value: GoalAnalysisEvidenceState,
) {
    val range = value.range.value ?: return
    val from = DateCompose.rememberFormat(
        value = range.from,
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    val to = DateCompose.rememberFormat(
        value = range.to,
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_analysis_period, from, to),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
        Text(
            text = AppTokens.strings.res(
                Res.string.goal_details_analysis_evidence,
                value.days,
                value.setCount,
            ),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun GoalAdjustmentSection(
    modifier: Modifier = Modifier,
    findings: ImmutableList<GoalFitFindingState>,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_fit_diagnostic_drifting_title),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )
        GoalFitDriftingCard(
            modifier = Modifier.fillMaxWidth(),
            findings = findings,
        )
    }
}

@Composable
private fun GoalAssessmentDisclosure(
    modifier: Modifier = Modifier,
    state: TrainingGoalDetailsDialogState,
    isExpanded: Boolean,
    onToggleDetails: () -> Unit,
) {
    val progress = state.progress ?: return
    val additionalFindings = state.additionalFindings
    val expansionDescription = AppTokens.strings.res(
        if (isExpanded) Res.string.goal_options_expanded else Res.string.goal_options_collapsed,
    )

    Column(modifier = modifier) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = expansionDescription },
            content = ButtonContent.Text(
                text = AppTokens.strings.res(
                    if (isExpanded) {
                        Res.string.goal_details_hide_details
                    } else {
                        Res.string.goal_details_show_details
                    },
                ),
                endIcon = ButtonIcon.Icon(
                    value = if (isExpanded) AppTokens.icons.ArrowTop else AppTokens.icons.ArrowDown,
                ),
            ),
            style = ButtonStyle.Tertiary,
            onClick = onToggleDetails,
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(DisclosureAnimationDurationMillis),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(DisclosureAnimationDurationMillis)),
            exit = shrinkVertically(
                animationSpec = tween(DisclosureAnimationDurationMillis),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(DisclosureAnimationDurationMillis)),
        ) {
            Column(
                modifier = Modifier.padding(top = AppTokens.dp.contentPadding.content),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                if (additionalFindings.any { it.severity != GoalFitSeverityState.PASS }) {
                    GoalFitDriftingCard(
                        modifier = Modifier.fillMaxWidth(),
                        findings = additionalFindings,
                    )
                }

                if (additionalFindings.any { it.severity == GoalFitSeverityState.PASS }) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    ) {
                        Text(
                            text = AppTokens.strings.res(Res.string.goal_fit_diagnostic_on_track_title),
                            style = AppTokens.typography.h4(),
                            color = AppTokens.colors.text.primary,
                        )
                        GoalFitOnTrackCard(
                            modifier = Modifier.fillMaxWidth(),
                            findings = additionalFindings,
                        )
                    }
                }

                GoalEvidenceDetails(
                    modifier = Modifier.fillMaxWidth(),
                    value = progress.evidence,
                )
            }
        }
    }
}

@Composable
private fun GoalEvidenceDetails(
    modifier: Modifier = Modifier,
    value: GoalAnalysisEvidenceState,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        if (value.range.value == null) {
            Text(
                text = AppTokens.strings.res(
                    Res.string.goal_details_analysis_evidence,
                    value.days,
                    value.setCount,
                ),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        if (value.excludedSetCount > 0) {
            Text(
                text = AppTokens.strings.res(
                    Res.string.goal_details_analysis_excluded,
                    value.excludedSetCount,
                ),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        if (value.missingMetadataSetCount > 0) {
            Text(
                text = AppTokens.strings.res(
                    Res.string.goal_details_analysis_missing_metadata,
                    value.missingMetadataSetCount,
                ),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_analysis_method),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

private fun populatedGoalDetailsPreview(): GoalProgressState = stubGoalProgress(
    primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
    score = 50,
    findings = persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.FAIL,
            actualValue = 1f,
            targetMin = 2f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
            severity = GoalFitSeverityState.WARN,
            actualValue = 9.6f,
            targetMin = 10f,
            targetMax = null,
            context = persistentListOf(MuscleGroupEnumState.CHEST_MUSCLES),
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
            severity = GoalFitSeverityState.WARN,
            actualValue = 9f,
            targetMin = 10f,
            targetMax = null,
            context = persistentListOf(MuscleGroupEnumState.BACK_MUSCLES),
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
            severity = GoalFitSeverityState.PASS,
            actualValue = 0.05f,
            targetMin = 0f,
            targetMax = null,
        ),
    ),
)

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenPreview() {
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = populatedGoalDetailsPreview(),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsWithoutProgressPreview() {
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
                progress = stubGoalProgress(score = 82),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenTooEarlyPreview() {
    val base = stubGoalProgress(score = null)
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
    val base = stubGoalProgress(score = null)
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
    val base = stubGoalProgress(primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE, score = 50)
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
    val base = stubGoalProgress(score = null)
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

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenOffTrackPreview() {
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = stubGoalProgress(score = 28),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenNearTargetPreview() {
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = stubGoalProgress(daysTotal = 30, daysElapsed = 28),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsScreenFinishedPreview() {
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = stubGoalProgress(daysTotal = 30, daysElapsed = 34),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsExpandedPreview() {
    val base = populatedGoalDetailsPreview()
    PreviewContainer {
        BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
            TrainingGoalDetailsContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, false),
                state = TrainingGoalDetailsDialogState(
                    progress = base.copy(
                        evidence = base.evidence.copy(
                            excludedSetCount = 3,
                            missingMetadataSetCount = 8,
                        ),
                    ),
                ),
                isExpanded = true,
                onToggleDetails = {},
                onAddGoal = TrainingGoalDetailsContract.Empty::onAddGoal,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingGoalDetailsPassOnlyPreview() {
    val base = stubGoalProgress(score = 100)
    PreviewContainer {
        TrainingGoalDetailsScreen(
            state = TrainingGoalDetailsDialogState(
                progress = base.copy(
                    findings = base.findings.filter { it.severity == GoalFitSeverityState.PASS }.toPersistentList(),
                ),
            ),
            loaders = persistentSetOf(),
            contract = TrainingGoalDetailsContract.Empty,
        )
    }
}
