package com.grippo.design.components.metrics.profile.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.TopExerciseContributionState
import com.grippo.core.state.metrics.profile.TopMuscleContributionState
import com.grippo.core.state.metrics.profile.stubGoalProgress
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
import com.grippo.design.resources.provider.goal_details_breakdown_exercise_sets
import com.grippo.design.resources.provider.goal_details_breakdown_sessions
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_hint
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_value
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_vs_target
import com.grippo.design.resources.provider.goal_details_breakdown_subtitle
import com.grippo.design.resources.provider.goal_details_breakdown_title
import com.grippo.design.resources.provider.goal_details_breakdown_top_exercises_empty
import com.grippo.design.resources.provider.goal_details_breakdown_top_exercises_title
import com.grippo.design.resources.provider.goal_details_breakdown_top_muscles_title
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_general
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_general_hint
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_hint
import com.grippo.design.resources.provider.goal_details_focus_endurance
import com.grippo.design.resources.provider.goal_details_focus_hypertrophy
import com.grippo.design.resources.provider.goal_details_focus_share
import com.grippo.design.resources.provider.goal_details_focus_strength

/**
 * Maximum number of top contributors rendered with a full progress bar in the
 * exercises and muscles blocks. Anything past this is rolled into a single
 * compact tail row to keep the card scannable on a phone.
 */
private const val TOP_EXERCISES_VISIBLE: Int = 3
private const val TOP_MUSCLES_VISIBLE: Int = 3

/**
 * Shows the raw training artifacts that drove the adherence score.
 *
 * The card is goal-aware and intentionally visual:
 *  - sessions are surfaced as a hero number,
 *  - the goal-specific quality row is rendered as a meter (or a two-tone
 *    work-balance bar for MAINTAIN / GENERAL_FITNESS),
 *  - top exercises and top muscles use full-width progress bars so users can
 *    compare contributions at a glance instead of parsing percentages.
 *
 * Each block is separated by a thin divider so the user can mentally chunk the
 * card into "how much I trained", "how I trained", "what I trained on".
 */
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
        BreakdownHeader(modifier = Modifier.fillMaxWidth())

        if (!value.hasBreakdown) {
            Spacer(Modifier.height(AppTokens.dp.contentPadding.block))
            EmptyBlock(modifier = Modifier.fillMaxWidth())
            return@Column
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

        SessionsBlock(
            modifier = Modifier.fillMaxWidth(),
            sessionCount = value.sessionCount,
            isConsistency = value.goal.secondaryGoal == GoalSecondaryGoalEnumState.CONSISTENCY,
        )

        if (value.shouldShowQualityBlock()) {
            SectionDivider(modifier = Modifier.fillMaxWidth())
            QualityBlock(
                modifier = Modifier.fillMaxWidth(),
                value = value,
            )
        }

        SectionDivider(modifier = Modifier.fillMaxWidth())
        TopExercisesBlock(
            modifier = Modifier.fillMaxWidth(),
            items = value.topExercises,
        )

        if (value.shouldShowMuscleFocus() && value.topMuscles.isNotEmpty()) {
            SectionDivider(modifier = Modifier.fillMaxWidth())
            TopMusclesBlock(
                modifier = Modifier.fillMaxWidth(),
                items = value.topMuscles,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Header
// -----------------------------------------------------------------------------

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

// -----------------------------------------------------------------------------
// Sessions hero block
// -----------------------------------------------------------------------------

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
        AppTokens.strings.res(
            Res.string.goal_details_breakdown_sessions_value,
            safeCount,
        )
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
                color = AppTokens.colors.text.primary,
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

// -----------------------------------------------------------------------------
// Quality block — single meter or two-tone work-balance bar.
// -----------------------------------------------------------------------------

@Composable
private fun QualityBlock(
    value: GoalProgressState,
    modifier: Modifier = Modifier,
) {
    when (value.goal.primaryGoal) {
        GoalPrimaryGoalEnumState.GET_STRONGER -> {
            CompoundQualityRow(
                modifier = modifier,
                share = value.compoundRatio,
            )
        }

        GoalPrimaryGoalEnumState.MAINTAIN -> {
            WorkBalanceRow(
                modifier = modifier,
                title = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance),
                hint = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_hint),
                leftLabel = AppTokens.strings.res(Res.string.goal_details_focus_strength),
                leftShare = value.strengthShare,
                leftColor = AppTokens.colors.charts.ring.warning.indicator,
                rightLabel = AppTokens.strings.res(Res.string.goal_details_focus_hypertrophy),
                rightShare = value.hypertrophyShare,
                rightColor = AppTokens.colors.charts.ring.info.indicator,
            )
        }

        GoalPrimaryGoalEnumState.GENERAL_FITNESS -> {
            WorkBalanceRow(
                modifier = modifier,
                title = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_general),
                hint = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_general_hint),
                leftLabel = AppTokens.strings.res(Res.string.goal_details_focus_strength),
                leftShare = value.strengthShare,
                leftColor = AppTokens.colors.charts.ring.warning.indicator,
                rightLabel = AppTokens.strings.res(Res.string.goal_details_focus_endurance),
                rightShare = value.enduranceShare,
                rightColor = AppTokens.colors.charts.ring.success.indicator,
            )
        }

        GoalPrimaryGoalEnumState.BUILD_MUSCLE,
        GoalPrimaryGoalEnumState.LOSE_FAT,
        GoalPrimaryGoalEnumState.RETURN_TO_TRAINING,
            -> Unit // Sessions + top contributors tell the full story.
    }
}

@Composable
private fun CompoundQualityRow(
    share: Int,
    modifier: Modifier = Modifier,
) {
    val accent = AppTokens.colors.charts.ring.warning.indicator
    val track = AppTokens.colors.charts.ring.muted.track

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
                text = AppTokens.strings.res(Res.string.goal_details_breakdown_compound),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = percentText(share),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ProgressBar(
            modifier = Modifier.fillMaxWidth(),
            fraction = (share.coerceIn(0, 100) / 100f),
            color = accent,
            trackColor = track,
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_compound_hint),
            style = AppTokens.typography.b11Reg(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun WorkBalanceRow(
    title: String,
    hint: String,
    leftLabel: String,
    leftShare: Int,
    leftColor: Color,
    rightLabel: String,
    rightShare: Int,
    rightColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BalanceSideLabel(
                label = leftLabel,
                share = leftShare,
                color = leftColor,
                alignEnd = false,
            )

            Spacer(Modifier.weight(1f))

            BalanceSideLabel(
                label = rightLabel,
                share = rightShare,
                color = rightColor,
                alignEnd = true,
            )
        }

        // The two declared dimensions usually don't sum to 100 (the rest of the
        // training stimulus goes to other qualities). Append a track-coloured
        // filler so the bar visually represents the full 100% scale and the
        // segment widths match the labelled percentages.
        val trackColor = AppTokens.colors.charts.ring.muted.track
        val filler = (100 - leftShare.coerceIn(0, 100) - rightShare.coerceIn(0, 100))
            .coerceAtLeast(0)

        BreakdownStackedBar(
            modifier = Modifier.fillMaxWidth(),
            segments = listOf(
                BreakdownBarSegment(weight = leftShare, color = leftColor),
                BreakdownBarSegment(weight = rightShare, color = rightColor),
                BreakdownBarSegment(weight = filler, color = trackColor),
            ),
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = hint,
            style = AppTokens.typography.b11Reg(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun BalanceSideLabel(
    label: String,
    share: Int,
    color: Color,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        if (!alignEnd) {
            LegendDot(color = color)
        }
        Text(
            text = label,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = percentText(share),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (alignEnd) {
            LegendDot(color = color)
        }
    }
}

// -----------------------------------------------------------------------------
// Top exercises block
// -----------------------------------------------------------------------------

@Composable
private fun TopExercisesBlock(
    items: List<TopExerciseContributionState>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_top_exercises_title),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )

        if (items.isEmpty()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.goal_details_breakdown_top_exercises_empty),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
            return@Column
        }

        // Cap visible rows at TOP_EXERCISES_VISIBLE so the card stays compact
        // and Pareto-friendly: the first three contributors usually carry most
        // of the score story, the long tail rolls into a single tail line.
        val visible = items.take(TOP_EXERCISES_VISIBLE)
        val tail = items.drop(TOP_EXERCISES_VISIBLE)

        visible.forEach { exercise ->
            TopExerciseRow(
                modifier = Modifier.fillMaxWidth(),
                item = exercise,
            )
        }

        if (tail.isNotEmpty()) {
            TopExercisesTailRow(
                modifier = Modifier.fillMaxWidth(),
                tail = tail,
            )
        }
    }
}

@Composable
private fun TopExercisesTailRow(
    tail: List<TopExerciseContributionState>,
    modifier: Modifier = Modifier,
) {
    val names = tail.joinToString(separator = ", ") { it.name }
    val tailShare = tail.sumOf { it.stimulusShare.coerceIn(0, 100) }.coerceIn(0, 100)

    Text(
        modifier = modifier,
        text = "+ $names · ${percentText(tailShare)}",
        style = AppTokens.typography.b11Reg(),
        color = AppTokens.colors.text.secondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TopExerciseRow(
    item: TopExerciseContributionState,
    modifier: Modifier = Modifier,
) {
    val accent = categoryColor(item.category)
    val track = AppTokens.colors.charts.ring.muted.track

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
                text = item.name,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = percentText(item.stimulusShare),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ProgressBar(
            modifier = Modifier.fillMaxWidth(),
            fraction = (item.stimulusShare.coerceIn(0, 100) / 100f),
            color = accent,
            trackColor = track,
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = buildExerciseSubline(item, accent),
            style = AppTokens.typography.b11Reg(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun buildExerciseSubline(
    item: TopExerciseContributionState,
    accent: Color,
): AnnotatedString {
    // Resolve every composable string up front — buildAnnotatedString /
    // withStyle lambdas are NOT composable scopes, so we can't call
    // AppTokens.strings.res or categoryLabel inside them.
    val setsText = AppTokens.strings.res(
        Res.string.goal_details_breakdown_exercise_sets,
        item.totalSets.coerceAtLeast(0),
    )
    val catLabel = item.category?.let { categoryLabel(it) }

    return buildAnnotatedString {
        append(setsText)
        if (catLabel != null) {
            append(" · ")
            // Tinting the category word the same colour as the bar gives an
            // implicit legend without the visual weight of a separate chip.
            withStyle(SpanStyle(color = accent)) {
                append(catLabel)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Top muscles block
// -----------------------------------------------------------------------------

@Composable
private fun TopMusclesBlock(
    items: List<TopMuscleContributionState>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_top_muscles_title),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )

        items.take(TOP_MUSCLES_VISIBLE).forEach { muscle ->
            TopMuscleRow(
                modifier = Modifier.fillMaxWidth(),
                item = muscle,
            )
        }
    }
}

@Composable
private fun TopMuscleRow(
    item: TopMuscleContributionState,
    modifier: Modifier = Modifier,
) {
    val accent = AppTokens.colors.charts.ring.info.indicator
    val track = AppTokens.colors.charts.ring.muted.track

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
                text = item.muscle.title().text(),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = percentText(item.share),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ProgressBar(
            modifier = Modifier.fillMaxWidth(),
            fraction = (item.share.coerceIn(0, 100) / 100f),
            color = accent,
            trackColor = track,
        )
    }
}

// -----------------------------------------------------------------------------
// Shared visual primitives
// -----------------------------------------------------------------------------

@Composable
private fun ProgressBar(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val height = AppTokens.dp.metrics.profile.goal.focusDistribution.barHeight
    val shape = RoundedCornerShape(height / 2)
    val safeFraction = fraction.coerceIn(0f, 1f)

    // Outer Box clips to the rounded shape so any inner fill picks up the
    // rounded edges from the clip — matches the segment pattern used by
    // BreakdownStackedBar in GoalFocusDistributionCard.
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(trackColor),
    ) {
        if (safeFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = safeFraction)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

@Immutable
private data class BreakdownBarSegment(
    val weight: Int,
    val color: Color,
)

@Composable
private fun BreakdownStackedBar(
    segments: List<BreakdownBarSegment>,
    modifier: Modifier = Modifier,
) {
    val barHeight = AppTokens.dp.metrics.profile.goal.focusDistribution.barHeight
    val shape = RoundedCornerShape(barHeight / 2)
    val total = segments.sumOf { it.weight.coerceAtLeast(0) }
    val fallbackColor = AppTokens.colors.charts.ring.muted.track

    Row(
        modifier = modifier
            .height(barHeight)
            .clip(shape)
            .background(fallbackColor, shape),
    ) {
        if (total <= 0) return@Row
        segments.forEach { segment ->
            val w = segment.weight.coerceAtLeast(0).toFloat() / total.toFloat()
            if (w <= 0f) return@forEach
            Box(
                modifier = Modifier
                    .weight(w)
                    .fillMaxHeight()
                    .background(segment.color),
            )
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AppTokens.dp.metrics.profile.goal.focusDistribution.legendDot)
            .clip(CircleShape)
            .background(color, CircleShape),
    )
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

// -----------------------------------------------------------------------------
// Helpers (non-UI)
// -----------------------------------------------------------------------------

@Composable
private fun percentText(share: Int): String {
    val raw = AppTokens.strings.res(Res.string.goal_details_focus_share, share.coerceIn(0, 100))
    return "$raw%"
}

@Composable
private fun categoryColor(category: CategoryEnumState?): Color {
    return when (category) {
        CategoryEnumState.COMPOUND -> AppTokens.colors.charts.ring.warning.indicator
        CategoryEnumState.ISOLATION -> AppTokens.colors.charts.ring.info.indicator
        null -> AppTokens.colors.charts.ring.muted.indicator
    }
}

@Composable
private fun categoryLabel(category: CategoryEnumState): String {
    return when (category) {
        CategoryEnumState.COMPOUND -> AppTokens.strings.res(Res.string.category_compound)
        CategoryEnumState.ISOLATION -> AppTokens.strings.res(Res.string.category_isolation)
    }
}

private fun GoalProgressState.shouldShowQualityBlock(): Boolean {
    return when (goal.primaryGoal) {
        GoalPrimaryGoalEnumState.GET_STRONGER,
        GoalPrimaryGoalEnumState.MAINTAIN,
        GoalPrimaryGoalEnumState.GENERAL_FITNESS,
            -> true

        else -> false
    }
}

private fun GoalProgressState.shouldShowMuscleFocus(): Boolean {
    return when (goal.primaryGoal) {
        GoalPrimaryGoalEnumState.BUILD_MUSCLE,
        GoalPrimaryGoalEnumState.MAINTAIN,
            -> true

        else -> false
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
private fun GoalCalculationBreakdownCardGeneralFitnessPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.GENERAL_FITNESS,
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
