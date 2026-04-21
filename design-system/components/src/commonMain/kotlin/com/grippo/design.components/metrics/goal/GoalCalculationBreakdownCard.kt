package com.grippo.design.components.metrics.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.core.state.metrics.TopExerciseContributionState
import com.grippo.core.state.metrics.TopMuscleContributionState
import com.grippo.core.state.metrics.stubGoalProgress
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.category_compound
import com.grippo.design.resources.provider.category_isolation
import com.grippo.design.resources.provider.goal_details_breakdown_compound
import com.grippo.design.resources.provider.goal_details_breakdown_compound_hint
import com.grippo.design.resources.provider.goal_details_breakdown_empty_detail
import com.grippo.design.resources.provider.goal_details_breakdown_empty_title
import com.grippo.design.resources.provider.goal_details_breakdown_exercise_heaviest
import com.grippo.design.resources.provider.goal_details_breakdown_exercise_one_rm
import com.grippo.design.resources.provider.goal_details_breakdown_exercise_sets
import com.grippo.design.resources.provider.goal_details_breakdown_exercise_share_caption
import com.grippo.design.resources.provider.goal_details_breakdown_muscle_share_caption
import com.grippo.design.resources.provider.goal_details_breakdown_sessions
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_hint
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_value
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_vs_target
import com.grippo.design.resources.provider.goal_details_breakdown_subtitle
import com.grippo.design.resources.provider.goal_details_breakdown_title
import com.grippo.design.resources.provider.goal_details_breakdown_top_exercises_empty
import com.grippo.design.resources.provider.goal_details_breakdown_top_exercises_hint
import com.grippo.design.resources.provider.goal_details_breakdown_top_exercises_title
import com.grippo.design.resources.provider.goal_details_breakdown_top_muscles_hint
import com.grippo.design.resources.provider.goal_details_breakdown_top_muscles_title
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_general
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_general_hint
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_hint
import com.grippo.design.resources.provider.goal_details_focus_share
import kotlin.math.roundToInt

/**
 * Shows the raw training artifacts that drove the adherence score: session
 * count, one headline quality row (or none, depending on goal), and the top
 * exercises that contributed the most stimulus. For hypertrophy / maintain
 * goals the top muscles block is appended as well.
 *
 * The component is goal-type aware and intentionally sparse — at most two
 * summary rows followed by the contributors list — so the user doesn't have
 * to parse a wall of numbers.
 */
@Composable
public fun GoalCalculationBreakdownCard(
    value: GoalProgressState,
    modifier: Modifier = Modifier,
) {
    val radius = AppTokens.dp.metrics.goal.focusDistribution.radius
    val shape = RoundedCornerShape(radius)

    Column(
        modifier = modifier
            .clip(shape)
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.metrics.goal.focusDistribution.horizontalPadding,
                vertical = AppTokens.dp.metrics.goal.focusDistribution.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.goal.focusDistribution.spacer),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_subtitle),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        if (!value.hasBreakdown) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))
            EmptyBlock()
            return@Column
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        SummaryRows(value = value)

        if (value.topExercises.isNotEmpty()) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))
            TopExercisesBlock(items = value.topExercises)
        } else {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))
            Text(
                text = AppTokens.strings.res(Res.string.goal_details_breakdown_top_exercises_empty),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }

        // Muscle breakdown is only rendered when it's relevant to the goal
        // (hypertrophy / maintain). The top-muscles list itself communicates
        // concentration — no separate "muscle focus %" needed.
        if (value.shouldShowMuscleFocus() && value.topMuscles.isNotEmpty()) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))
            TopMusclesBlock(items = value.topMuscles)
        }
    }
}

// -----------------------------------------------------------------------------
// Summary rows — at most two rows, picked from the active goal.
// -----------------------------------------------------------------------------

@Composable
private fun SummaryRows(
    value: GoalProgressState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
        // Session count is universal. For the CONSISTENCY secondary goal we
        // show "N of M target" phrasing, otherwise just the raw count.
        val isConsistency = value.goal.secondaryGoal == GoalSecondaryGoalEnumState.CONSISTENCY
        SummaryRow(
            label = AppTokens.strings.res(Res.string.goal_details_breakdown_sessions),
            value = if (isConsistency) {
                AppTokens.strings.res(
                    Res.string.goal_details_breakdown_sessions_vs_target,
                    value.sessionCount.coerceAtLeast(0),
                    GoalProgressState.CONSISTENCY_TARGET_SESSIONS,
                )
            } else {
                AppTokens.strings.res(
                    Res.string.goal_details_breakdown_sessions_value,
                    value.sessionCount.coerceAtLeast(0),
                )
            },
            hint = AppTokens.strings.res(Res.string.goal_details_breakdown_sessions_hint),
        )

        // Goal-specific quality row. We show at most one — the breakdown stays
        // cognitively simple and leans on the top-exercises list below for
        // concrete evidence.
        when (value.goal.primaryGoal) {
            GoalPrimaryGoalEnumState.GET_STRONGER -> {
                SummaryRow(
                    label = AppTokens.strings.res(Res.string.goal_details_breakdown_compound),
                    value = percentText(value.compoundRatio),
                    hint = AppTokens.strings.res(Res.string.goal_details_breakdown_compound_hint),
                )
            }

            GoalPrimaryGoalEnumState.MAINTAIN -> {
                SummaryRow(
                    label = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance),
                    value = "${percentText(value.strengthShare)} / ${percentText(value.hypertrophyShare)}",
                    hint = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_hint),
                )
            }

            GoalPrimaryGoalEnumState.GENERAL_FITNESS -> {
                SummaryRow(
                    label = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_general),
                    value = "${percentText(value.strengthShare)} / ${percentText(value.enduranceShare)}",
                    hint = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_general_hint),
                )
            }

            GoalPrimaryGoalEnumState.BUILD_MUSCLE,
            GoalPrimaryGoalEnumState.LOSE_FAT,
            GoalPrimaryGoalEnumState.RETURN_TO_TRAINING,
                -> Unit // Sessions + top contributors tell the full story here.
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    hint: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = value,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!hint.isNullOrEmpty()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = hint,
                style = AppTokens.typography.b11Reg(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Top exercises block
// -----------------------------------------------------------------------------

@Composable
private fun TopExercisesBlock(
    items: List<TopExerciseContributionState>,
) {
    val topCount = items.size
    val topShareSum = items.sumOf { it.stimulusShare.coerceIn(0, 100) }
    val remainderShare = (100 - topShareSum).coerceIn(0, 100)

    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_top_exercises_title),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = AppTokens.strings.res(
                Res.string.goal_details_breakdown_top_exercises_hint,
                topCount,
                remainderShare,
            ),
            style = AppTokens.typography.b11Reg(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        items.forEach { exercise ->
            TopExerciseRow(item = exercise)
        }
    }
}

@Composable
private fun TopExerciseRow(
    item: TopExerciseContributionState,
) {
    val accent = categoryColor(item.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.metrics.goal.focusDistribution.legendDot)
                .clip(CircleShape)
                .background(accent, CircleShape),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = buildExerciseSubline(item),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = percentText(item.stimulusShare),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = AppTokens.strings.res(Res.string.goal_details_breakdown_exercise_share_caption),
                style = AppTokens.typography.b11Reg(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun buildExerciseSubline(item: TopExerciseContributionState): String {
    val parts = mutableListOf<String>()
    parts += AppTokens.strings.res(
        Res.string.goal_details_breakdown_exercise_sets,
        item.totalSets.coerceAtLeast(0),
    )

    val oneRm = item.estimatedOneRepMax
    when {
        oneRm.isFinite() && oneRm > 1f -> {
            parts += AppTokens.strings.res(
                Res.string.goal_details_breakdown_exercise_one_rm,
                oneRm.roundToInt(),
            )
        }

        item.heaviestWeight.isFinite() && item.heaviestWeight > 0f -> {
            parts += AppTokens.strings.res(
                Res.string.goal_details_breakdown_exercise_heaviest,
                item.heaviestWeight.roundToInt(),
            )
        }
    }

    item.category?.let { category ->
        val label = when (category) {
            CategoryEnumState.COMPOUND -> AppTokens.strings.res(Res.string.category_compound)
            CategoryEnumState.ISOLATION -> AppTokens.strings.res(Res.string.category_isolation)
        }
        parts += label
    }

    return parts.joinToString(separator = " · ")
}

@Composable
private fun categoryColor(category: CategoryEnumState?): Color {
    return when (category) {
        CategoryEnumState.COMPOUND -> AppTokens.colors.charts.ring.warning.indicator
        CategoryEnumState.ISOLATION -> AppTokens.colors.charts.ring.info.indicator
        null -> AppTokens.colors.charts.ring.muted.indicator
    }
}

// -----------------------------------------------------------------------------
// Top muscles block
// -----------------------------------------------------------------------------

@Composable
private fun TopMusclesBlock(
    items: List<TopMuscleContributionState>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_top_muscles_title),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_top_muscles_hint),
            style = AppTokens.typography.b11Reg(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        items.forEach { muscle ->
            MuscleRow(item = muscle)
        }
    }
}

@Composable
private fun MuscleRow(
    item: TopMuscleContributionState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.metrics.goal.focusDistribution.legendDot)
                .clip(CircleShape)
                .background(AppTokens.colors.charts.ring.info.indicator, CircleShape),
        )

        Text(
            modifier = Modifier.weight(1f),
            text = item.muscle.title().text(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val barHeight = AppTokens.dp.metrics.goal.focusDistribution.barHeight
        val barShape = RoundedCornerShape(barHeight / 2)
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.3f)
                .height(barHeight)
                .clip(barShape)
                .background(AppTokens.colors.charts.ring.muted.track, barShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (item.share.coerceIn(0, 100) / 100f))
                    .height(barHeight)
                    .background(AppTokens.colors.charts.ring.info.indicator, barShape),
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = percentText(item.share),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = AppTokens.strings.res(Res.string.goal_details_breakdown_muscle_share_caption),
                style = AppTokens.typography.b11Reg(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------------

@Composable
private fun percentText(share: Int): String {
    return AppTokens.strings.res(Res.string.goal_details_focus_share, share.coerceIn(0, 100))
}

private fun GoalProgressState.shouldShowMuscleFocus(): Boolean {
    return when (goal.primaryGoal) {
        GoalPrimaryGoalEnumState.BUILD_MUSCLE,
        GoalPrimaryGoalEnumState.MAINTAIN,
            -> true

        else -> false
    }
}

@Composable
private fun EmptyBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_empty_title),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_empty_detail),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

// -----------------------------------------------------------------------------
// Previews
// -----------------------------------------------------------------------------

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardStrengthPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.GET_STRONGER,
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardHypertrophyPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardMaintainPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.MAINTAIN,
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardRttPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.RETURN_TO_TRAINING,
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardConsistencyPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
                secondary = GoalSecondaryGoalEnumState.CONSISTENCY,
            ),
        )
    }
}
