package com.grippo.design.components.metrics.profile.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalFitFindings
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.design.components.metrics.profile.goal.internal.GoalFitDiagnosticBlock
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_details_breakdown_empty_detail
import com.grippo.design.resources.provider.goal_details_breakdown_empty_title
import com.grippo.design.resources.provider.goal_details_breakdown_sessions
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_hint
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_value
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_vs_target
import com.grippo.design.resources.provider.goal_details_breakdown_subtitle
import com.grippo.design.resources.provider.goal_details_breakdown_title
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun GoalCalculationBreakdownCard(
    value: GoalProgressState,
    modifier: Modifier = Modifier,
) {
    val radius = AppTokens.dp.metrics.profile.goal.focusDistribution.radius
    val shape = RoundedCornerShape(radius)

    Column(
        modifier = modifier
            .clip(shape)
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.metrics.profile.goal.focusDistribution.horizontalPadding,
                vertical = AppTokens.dp.metrics.profile.goal.focusDistribution.verticalPadding,
            ),
    ) {
        BreakdownHeader(
            modifier = Modifier.fillMaxWidth()
        )

        if (!value.hasBreakdown) {
            Spacer(Modifier.height(AppTokens.dp.contentPadding.block))
            EmptyBlock(modifier = Modifier.fillMaxWidth())
            return@Column
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

        SessionsBlock(
            sessionCount = value.sessionCount,
            isConsistency = value.goal.secondaryGoal == GoalSecondaryGoalEnumState.CONSISTENCY,
            modifier = Modifier.fillMaxWidth(),
        )

        if (value.findings.isNotEmpty()) {
            SectionDivider(modifier = Modifier.fillMaxWidth())
            GoalFitDiagnosticBlock(
                findings = value.findings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BreakdownHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_subtitle),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun SessionsBlock(
    sessionCount: Int,
    isConsistency: Boolean,
    modifier: Modifier = Modifier,
) {
    val safeCount = sessionCount.coerceAtLeast(0)
    val valueText = if (isConsistency) {
        AppTokens.strings.res(
            Res.string.goal_details_breakdown_sessions_vs_target,
            safeCount,
            GoalProgressState.CONSISTENCY_TARGET_SESSIONS,
        )
    } else {
        AppTokens.strings.res(Res.string.goal_details_breakdown_sessions_value, safeCount)
    }

    val valueColor = when {
        isConsistency && safeCount >= GoalProgressState.CONSISTENCY_TARGET_SESSIONS ->
            AppTokens.colors.semantic.success

        isConsistency ->
            AppTokens.colors.semantic.error

        else ->
            AppTokens.colors.text.primary
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.goal_details_breakdown_sessions),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = valueText,
                style = AppTokens.typography.h6(),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_sessions_hint),
            style = AppTokens.typography.b11Reg(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun SectionDivider(modifier: Modifier = Modifier) {
    Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
    HorizontalDivider(
        modifier = modifier,
        color = AppTokens.colors.divider.default,
    )
    Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
}

@Composable
private fun EmptyBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_empty_title),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_empty_detail),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun GoalFitBuildMusclePreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
                sessionCount = 18,
                findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.BUILD_MUSCLE),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitGetStrongerPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.GET_STRONGER,
                sessionCount = 12,
                findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.GET_STRONGER),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitMaintainPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.MAINTAIN,
                findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.MAINTAIN),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitGeneralFitnessPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.GENERAL_FITNESS,
                findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.GENERAL_FITNESS),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitLoseFatPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.LOSE_FAT,
                sessionCount = 1,
                findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.LOSE_FAT),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitReturnToTrainingPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.RETURN_TO_TRAINING,
                sessionCount = 20,
                findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.RETURN_TO_TRAINING),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitEmptyDataPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
                sessionCount = 0,
                topExercises = persistentListOf(),
                topMuscles = persistentListOf(),
                topMuscleGroups = persistentListOf(),
                findings = persistentListOf(),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitConsistencyPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
                secondary = GoalSecondaryGoalEnumState.CONSISTENCY,
                sessionCount = 2,
                findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.BUILD_MUSCLE),
            ),
        )
    }
}
