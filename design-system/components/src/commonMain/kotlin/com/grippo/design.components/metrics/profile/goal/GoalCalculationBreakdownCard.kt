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
import com.grippo.core.state.muscles.MuscleEnumState
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
import com.grippo.design.resources.provider.goal_details_breakdown_muscle_coverage
import com.grippo.design.resources.provider.goal_details_breakdown_sessions
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_hint
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_value
import com.grippo.design.resources.provider.goal_details_breakdown_sessions_vs_target
import com.grippo.design.resources.provider.goal_details_breakdown_subtitle
import com.grippo.design.resources.provider.goal_details_breakdown_title
import com.grippo.design.resources.provider.goal_details_breakdown_top_exercises_empty
import com.grippo.design.resources.provider.goal_details_breakdown_top_exercises_title
import com.grippo.design.resources.provider.goal_details_breakdown_top_muscles_title
import com.grippo.design.resources.provider.goal_details_breakdown_undercoverage
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_general
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_general_hint
import com.grippo.design.resources.provider.goal_details_breakdown_work_balance_hint
import com.grippo.design.resources.provider.goal_details_focus_endurance
import com.grippo.design.resources.provider.goal_details_focus_hypertrophy
import com.grippo.design.resources.provider.goal_details_focus_share
import com.grippo.design.resources.provider.goal_details_focus_strength
import com.grippo.design.resources.provider.muscle_group_back_muscles
import com.grippo.design.resources.provider.muscle_group_chest_muscles
import kotlinx.collections.immutable.persistentListOf

/**
 * Maximum number of top contributors rendered with a full progress bar in the
 * exercises and muscles blocks. Exercises stay tight at three rows because the
 * tail line is doing most of the work for GET_STRONGER, while muscles get a
 * larger window because, for hypertrophy goals, coverage is the signal.
 */
private const val TOP_EXERCISES_VISIBLE: Int = 3
private const val TOP_MUSCLES_VISIBLE: Int = 6

/**
 * Below this share of compound work, a GET_STRONGER goal is treated as
 * misaligned — the user is leaning too much on isolation lifts to drive
 * strength gains, and the compound row switches to the error palette.
 */
private const val COMPOUND_ALIGNMENT_THRESHOLD: Int = 50

/**
 * A major-muscle anchor (chest, back, quads, hams, glutes) with combined share
 * below this threshold reads as a coverage gap and surfaces in the
 * undercoverage callout.
 */
private const val MAJOR_MUSCLE_UNDERCOVERAGE_THRESHOLD: Int = 5

/**
 * MAINTAIN / GENERAL_FITNESS quality bar splits two declared dimensions. When
 * one side absorbs more than this fraction of the declared work, the dominant
 * side reads as misaligned and picks up the error palette.
 */
private const val WORK_BALANCE_SKEW_THRESHOLD: Float = 0.7f

/**
 * Shows the raw training artifacts that drove the adherence score.
 *
 * The card is goal-aware in the strict sense: which blocks render at all is a
 * function of the primary goal, so the user only sees signal that's
 * actionable for what they're training for.
 *
 *  - GET_STRONGER → sessions, compound meter, top exercises (isolation reads
 *    red).
 *  - BUILD_MUSCLE → sessions, muscles with coverage stat, accessory inversion
 *    flag, undercoverage callout. No quality block, no exercise list — for
 *    hypertrophy a single hero lift isn't a concept.
 *  - MAINTAIN → sessions, strength/hypertrophy split (skew flagged), muscles
 *    with the same coverage / inversion signals as BUILD_MUSCLE.
 *  - GENERAL_FITNESS → sessions, strength/endurance split (skew flagged), top
 *    exercises.
 *  - LOSE_FAT / RETURN_TO_TRAINING → sessions only. Adherence is the signal,
 *    everything else would be noise.
 *
 * Each block is separated by a thin divider; dividers only render between
 * adjacent visible blocks so a goal that hides a block never shows a leading
 * or doubled separator.
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

        // SessionsBlock is the always-on anchor for non-empty breakdowns: it
        // never gets a leading divider. Each subsequent block prefixes itself
        // with a divider only when it actually renders, which keeps the
        // "no leading, no doubled" invariant the spec calls for.
        SessionsBlock(
            sessionCount = value.sessionCount,
            isConsistency = value.goal.secondaryGoal == GoalSecondaryGoalEnumState.CONSISTENCY,
            modifier = Modifier.fillMaxWidth(),
        )

        if (value.shouldShowQualityBlock()) {
            SectionDivider(modifier = Modifier.fillMaxWidth())
            QualityBlock(
                value = value,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (value.shouldShowExercisesBlock()) {
            SectionDivider(modifier = Modifier.fillMaxWidth())
            TopExercisesBlock(
                items = value.topExercises,
                primaryGoal = value.goal.primaryGoal,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (value.shouldShowMuscleFocus()) {
            SectionDivider(modifier = Modifier.fillMaxWidth())
            TopMusclesBlock(
                items = value.topMuscles,
                modifier = Modifier.fillMaxWidth(),
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

    // For a CONSISTENCY secondary goal the session count has a clear pass/fail:
    // hit the target → success colour (rare green-light moment); miss it → the
    // error palette so the user sees adherence is the thing to fix.
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
                share = value.compoundRatio,
                modifier = modifier,
            )
        }

        GoalPrimaryGoalEnumState.MAINTAIN -> {
            WorkBalanceRow(
                title = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance),
                hint = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_hint),
                leftLabel = AppTokens.strings.res(Res.string.goal_details_focus_strength),
                leftShare = value.strengthShare,
                leftColor = AppTokens.colors.charts.ring.warning.indicator,
                rightLabel = AppTokens.strings.res(Res.string.goal_details_focus_hypertrophy),
                rightShare = value.hypertrophyShare,
                rightColor = AppTokens.colors.charts.ring.info.indicator,
                modifier = modifier,
            )
        }

        GoalPrimaryGoalEnumState.GENERAL_FITNESS -> {
            WorkBalanceRow(
                title = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_general),
                hint = AppTokens.strings.res(Res.string.goal_details_breakdown_work_balance_general_hint),
                leftLabel = AppTokens.strings.res(Res.string.goal_details_focus_strength),
                leftShare = value.strengthShare,
                leftColor = AppTokens.colors.charts.ring.warning.indicator,
                rightLabel = AppTokens.strings.res(Res.string.goal_details_focus_endurance),
                rightShare = value.enduranceShare,
                rightColor = AppTokens.colors.charts.ring.success.indicator,
                modifier = modifier,
            )
        }

        GoalPrimaryGoalEnumState.BUILD_MUSCLE,
        GoalPrimaryGoalEnumState.LOSE_FAT,
        GoalPrimaryGoalEnumState.RETURN_TO_TRAINING,
            -> Unit // Other blocks tell the full story for these goals.
    }
}

@Composable
private fun CompoundQualityRow(
    share: Int,
    modifier: Modifier = Modifier,
) {
    // Below the threshold the user is leaning too hard on isolation work, which
    // doesn't drive strength gains. Switch the bar and the percent number to
    // the error palette so the block reads as a defect at a glance.
    val onTrack = share >= COMPOUND_ALIGNMENT_THRESHOLD
    val accent = if (onTrack) {
        AppTokens.colors.charts.ring.warning.indicator
    } else {
        AppTokens.colors.semantic.error
    }
    val percentColor = if (onTrack) {
        AppTokens.colors.text.primary
    } else {
        AppTokens.colors.semantic.error
    }
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
                color = percentColor,
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
    val safeLeft = leftShare.coerceIn(0, 100)
    val safeRight = rightShare.coerceIn(0, 100)
    val declared = safeLeft + safeRight

    // When one side absorbs more than WORK_BALANCE_SKEW_THRESHOLD of the
    // declared work, we treat the user as having dropped the other dimension
    // and tint the dominant side red. We deliberately compute the ratio
    // against (left + right), not 100 — the rest of the stimulus is filler
    // that doesn't belong to either named dimension.
    val skewedToLeft = declared > 0 &&
            (safeLeft.toFloat() / declared.toFloat()) > WORK_BALANCE_SKEW_THRESHOLD
    val skewedToRight = declared > 0 &&
            (safeRight.toFloat() / declared.toFloat()) > WORK_BALANCE_SKEW_THRESHOLD

    val leftLabelColor =
        if (skewedToLeft) AppTokens.colors.semantic.error else AppTokens.colors.text.primary
    val leftValueColor =
        if (skewedToLeft) AppTokens.colors.semantic.error else AppTokens.colors.text.secondary
    val rightLabelColor =
        if (skewedToRight) AppTokens.colors.semantic.error else AppTokens.colors.text.primary
    val rightValueColor =
        if (skewedToRight) AppTokens.colors.semantic.error else AppTokens.colors.text.secondary

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
                share = safeLeft,
                color = leftColor,
                alignEnd = false,
                labelColor = leftLabelColor,
                valueColor = leftValueColor,
            )

            Spacer(Modifier.weight(1f))

            BalanceSideLabel(
                label = rightLabel,
                share = safeRight,
                color = rightColor,
                alignEnd = true,
                labelColor = rightLabelColor,
                valueColor = rightValueColor,
            )
        }

        // The two declared dimensions usually don't sum to 100 (the rest of the
        // training stimulus goes to other qualities). Append a track-coloured
        // filler so the bar visually represents the full 100% scale and the
        // segment widths match the labelled percentages.
        val trackColor = AppTokens.colors.charts.ring.muted.track
        val filler = (100 - safeLeft - safeRight).coerceAtLeast(0)

        BreakdownStackedBar(
            segments = listOf(
                BreakdownBarSegment(weight = safeLeft, color = leftColor),
                BreakdownBarSegment(weight = safeRight, color = rightColor),
                BreakdownBarSegment(weight = filler, color = trackColor),
            ),
            modifier = Modifier.fillMaxWidth(),
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
    labelColor: Color,
    valueColor: Color,
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
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = percentText(share),
            style = AppTokens.typography.b13Med(),
            color = valueColor,
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
    primaryGoal: GoalPrimaryGoalEnumState,
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
                item = exercise,
                primaryGoal = primaryGoal,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (tail.isNotEmpty()) {
            TopExercisesTailRow(
                tail = tail,
                modifier = Modifier.fillMaxWidth(),
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
    primaryGoal: GoalPrimaryGoalEnumState,
    modifier: Modifier = Modifier,
) {
    val accent = exerciseAccent(item.category, primaryGoal)
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
// Top muscles block — coverage stat + inversion + undercoverage callout.
// -----------------------------------------------------------------------------

@Composable
private fun TopMusclesBlock(
    items: List<TopMuscleContributionState>,
    modifier: Modifier = Modifier,
) {
    val visible = items.take(TOP_MUSCLES_VISIBLE)
    val coverageCount = items.count { it.share > 0 }

    // Inversion: an accessory whose share beats every primary-mover's share
    // reads as "the small muscles are getting more love than the large
    // groups", which for hypertrophy is a programming red flag. If primaries
    // are entirely missing from the visible list we don't blanket-flag — the
    // undercoverage callout will already explain the coverage gap.
    val maxPrimaryShare = items
        .filter { it.muscle.breakdownRole() == BreakdownMuscleRole.PRIMARY }
        .maxOfOrNull { it.share }
    val invertedMuscles: Set<MuscleEnumState> = if (maxPrimaryShare == null) {
        emptySet()
    } else {
        items.asSequence()
            .filter {
                it.muscle.breakdownRole() == BreakdownMuscleRole.ACCESSORY &&
                        it.share > maxPrimaryShare
            }
            .map { it.muscle }
            .toSet()
    }

    // Aggregate visible share per major-muscle anchor. Anchors below the
    // undercoverage threshold (including those not present at all → 0%) get
    // listed in a single callout below the muscle list.
    val undercovered: List<UndercoveredAnchor> = BreakdownMajorMuscle.entries
        .map { anchor ->
            val total = items
                .filter { it.muscle in anchor.members }
                .sumOf { it.share.coerceAtLeast(0) }
            UndercoveredAnchor(anchor = anchor, share = total)
        }
        .filter { it.share < MAJOR_MUSCLE_UNDERCOVERAGE_THRESHOLD }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        TopMusclesHeader(
            coverageCount = coverageCount,
            modifier = Modifier.fillMaxWidth(),
        )

        visible.forEach { muscle ->
            TopMuscleRow(
                item = muscle,
                isInverted = muscle.muscle in invertedMuscles,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (undercovered.isNotEmpty()) {
            UndercoverageCallout(
                items = undercovered,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TopMusclesHeader(
    coverageCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_details_breakdown_top_muscles_title),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(
                Res.string.goal_details_breakdown_muscle_coverage,
                coverageCount.coerceAtLeast(0),
            ),
            style = AppTokens.typography.b11Reg(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun TopMuscleRow(
    item: TopMuscleContributionState,
    isInverted: Boolean,
    modifier: Modifier = Modifier,
) {
    // When an accessory outranks every primary mover, the row picks up the
    // error palette — the bar and the percent both read red so the inversion
    // is visually impossible to miss while the muscle name itself stays
    // readable in the primary text colour (matches the GET_STRONGER pattern).
    val accent = if (isInverted) {
        AppTokens.colors.semantic.error
    } else {
        AppTokens.colors.charts.ring.info.indicator
    }
    val percentColor = if (isInverted) {
        AppTokens.colors.semantic.error
    } else {
        AppTokens.colors.text.primary
    }
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
                color = percentColor,
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

@Composable
private fun UndercoverageCallout(
    items: List<UndercoveredAnchor>,
    modifier: Modifier = Modifier,
) {
    // Resolve composable strings up front so the joinToString / template
    // call sites stay in a normal scope.
    val labels = items.map { it.anchor.label() }
    val combined = items
        .sumOf { it.share.coerceAtLeast(0) }
        .coerceIn(0, 100)
    val combinedPercent = percentText(combined)
    val joined = labels.joinToString(separator = ", ")

    val message = AppTokens.strings.res(
        Res.string.goal_details_breakdown_undercoverage,
        joined,
        combinedPercent,
    )

    Text(
        modifier = modifier,
        text = message,
        style = AppTokens.typography.b11Reg(),
        color = AppTokens.colors.semantic.notice,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
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

/**
 * Goal-aware accent for an exercise row.
 *
 * For GET_STRONGER, isolation work isn't pushing the score forward, so it
 * gets the error palette — the user spots misaligned rows visually without
 * having to read the subline. For every other goal the category mapping is
 * descriptive, not judgmental, so we fall back to the neutral category palette.
 */
@Composable
private fun exerciseAccent(
    category: CategoryEnumState?,
    primaryGoal: GoalPrimaryGoalEnumState,
): Color {
    if (primaryGoal == GoalPrimaryGoalEnumState.GET_STRONGER &&
        category == CategoryEnumState.ISOLATION
    ) {
        return AppTokens.colors.semantic.error
    }
    return categoryColor(category)
}

@Composable
private fun categoryLabel(category: CategoryEnumState): String {
    return when (category) {
        CategoryEnumState.COMPOUND -> AppTokens.strings.res(Res.string.category_compound)
        CategoryEnumState.ISOLATION -> AppTokens.strings.res(Res.string.category_isolation)
    }
}

private fun GoalProgressState.shouldShowQualityBlock(): Boolean = when (goal.primaryGoal) {
    GoalPrimaryGoalEnumState.GET_STRONGER,
    GoalPrimaryGoalEnumState.MAINTAIN,
    GoalPrimaryGoalEnumState.GENERAL_FITNESS,
        -> true

    else -> false
}

private fun GoalProgressState.shouldShowExercisesBlock(): Boolean {
    if (topExercises.isEmpty()) return false
    return when (goal.primaryGoal) {
        // Strength is driven by individual heavy lifts, and general-fitness
        // users still want a sense of "what am I leaning on most" — both keep
        // the exercise contributors. Hypertrophy and maintain users get
        // muscle-group signal instead, and adherence-only goals (LOSE_FAT,
        // RETURN_TO_TRAINING) drop the block entirely.
        GoalPrimaryGoalEnumState.GET_STRONGER,
        GoalPrimaryGoalEnumState.GENERAL_FITNESS,
            -> true

        else -> false
    }
}

private fun GoalProgressState.shouldShowMuscleFocus(): Boolean {
    if (topMuscles.isEmpty()) return false
    return when (goal.primaryGoal) {
        GoalPrimaryGoalEnumState.BUILD_MUSCLE,
        GoalPrimaryGoalEnumState.MAINTAIN,
            -> true

        else -> false
    }
}

// -----------------------------------------------------------------------------
// Muscle classification (for the inversion + undercoverage signals).
//
// Defined locally because MuscleEnumState is purely anatomical and doesn't
// expose the "primary mover vs accessory" role this card cares about. The
// classification mirrors the spec's anatomy buckets, mapped onto the actual
// enum entries the project ships with.
// -----------------------------------------------------------------------------

private enum class BreakdownMuscleRole { PRIMARY, ACCESSORY }

private fun MuscleEnumState.breakdownRole(): BreakdownMuscleRole = when (this) {
    // Primary movers — large groups a hypertrophy / maintenance program is
    // expected to anchor on.
    MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
    MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL,
    MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL,
    MuscleEnumState.LATISSIMUS_DORSI,
    MuscleEnumState.TRAPEZIUS,
    MuscleEnumState.RHOMBOIDS,
    MuscleEnumState.QUADRICEPS,
    MuscleEnumState.HAMSTRINGS,
    MuscleEnumState.GLUTEAL,
        -> BreakdownMuscleRole.PRIMARY

    // Accessory — small groups that piggy-back on primary work. An accessory
    // out-ranking every primary is the inversion the card flags.
    MuscleEnumState.TERES_MAJOR,
    MuscleEnumState.RECTUS_ABDOMINIS,
    MuscleEnumState.OBLIQUES,
    MuscleEnumState.CALF,
    MuscleEnumState.ADDUCTORS,
    MuscleEnumState.ABDUCTORS,
    MuscleEnumState.ANTERIOR_DELTOID,
    MuscleEnumState.LATERAL_DELTOID,
    MuscleEnumState.POSTERIOR_DELTOID,
    MuscleEnumState.BICEPS,
    MuscleEnumState.TRICEPS,
    MuscleEnumState.FOREARM,
        -> BreakdownMuscleRole.ACCESSORY
}

/**
 * Major-muscle anchors a hypertrophy / maintenance program is expected to
 * cover. Each anchor aggregates one or more `MuscleEnumState` entries; the
 * card sums their visible share and marks the anchor as undercovered when the
 * total falls below `MAJOR_MUSCLE_UNDERCOVERAGE_THRESHOLD`.
 */
private enum class BreakdownMajorMuscle(
    val members: Set<MuscleEnumState>,
) {
    CHEST(
        members = setOf(
            MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
            MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL,
            MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL,
        ),
    ),
    BACK(members = setOf(MuscleEnumState.LATISSIMUS_DORSI)),
    QUADRICEPS(members = setOf(MuscleEnumState.QUADRICEPS)),
    HAMSTRINGS(members = setOf(MuscleEnumState.HAMSTRINGS)),
    GLUTES(members = setOf(MuscleEnumState.GLUTEAL)),
}

@Composable
private fun BreakdownMajorMuscle.label(): String = when (this) {
    BreakdownMajorMuscle.CHEST -> AppTokens.strings.res(Res.string.muscle_group_chest_muscles)
    BreakdownMajorMuscle.BACK -> AppTokens.strings.res(Res.string.muscle_group_back_muscles)
    BreakdownMajorMuscle.QUADRICEPS -> MuscleEnumState.QUADRICEPS.title().text()
    BreakdownMajorMuscle.HAMSTRINGS -> MuscleEnumState.HAMSTRINGS.title().text()
    BreakdownMajorMuscle.GLUTES -> MuscleEnumState.GLUTEAL.title().text()
}

@Immutable
private data class UndercoveredAnchor(
    val anchor: BreakdownMajorMuscle,
    val share: Int,
)

// -----------------------------------------------------------------------------
// Previews
// -----------------------------------------------------------------------------

private fun balancedHypertrophyMuscles() = persistentListOf(
    TopMuscleContributionState(MuscleEnumState.QUADRICEPS, 18),
    TopMuscleContributionState(MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL, 16),
    TopMuscleContributionState(MuscleEnumState.LATISSIMUS_DORSI, 14),
    TopMuscleContributionState(MuscleEnumState.GLUTEAL, 12),
    TopMuscleContributionState(MuscleEnumState.HAMSTRINGS, 10),
    TopMuscleContributionState(MuscleEnumState.TRICEPS, 8),
    TopMuscleContributionState(MuscleEnumState.BICEPS, 7),
)

private fun invertedHypertrophyMuscles() = persistentListOf(
    // Triceps (accessory) outranks every primary mover present, and quads /
    // hams / glutes are entirely absent — both signals fire.
    TopMuscleContributionState(MuscleEnumState.TRICEPS, 16),
    TopMuscleContributionState(MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL, 10),
    TopMuscleContributionState(MuscleEnumState.TRAPEZIUS, 9),
    TopMuscleContributionState(MuscleEnumState.BICEPS, 8),
    TopMuscleContributionState(MuscleEnumState.LATISSIMUS_DORSI, 6),
    TopMuscleContributionState(MuscleEnumState.ANTERIOR_DELTOID, 6),
)

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
private fun GoalCalculationBreakdownCardStrengthMisalignedPreview() {
    // Stress-test the error palette: sessions miss the consistency target,
    // compound share is below the alignment threshold, and two of the top
    // three contributors are isolation lifts. The card should read as a
    // sequence of red callouts.
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.GET_STRONGER,
                sessionCount = 1,
                compoundRatio = 35,
                topExercises = persistentListOf(
                    TopExerciseContributionState(
                        exampleId = "mis-1",
                        name = "Back Squat",
                        totalSets = 6,
                        stimulusShare = 22,
                        heaviestWeight = 110f,
                        estimatedOneRepMax = 125f,
                        category = CategoryEnumState.COMPOUND,
                    ),
                    TopExerciseContributionState(
                        exampleId = "mis-2",
                        name = "Bicep Curl",
                        totalSets = 14,
                        stimulusShare = 18,
                        heaviestWeight = 22f,
                        estimatedOneRepMax = 26f,
                        category = CategoryEnumState.ISOLATION,
                    ),
                    TopExerciseContributionState(
                        exampleId = "mis-3",
                        name = "Lateral Raise",
                        totalSets = 12,
                        stimulusShare = 14,
                        heaviestWeight = 12f,
                        estimatedOneRepMax = 14f,
                        category = CategoryEnumState.ISOLATION,
                    ),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardHypertrophyPreview() {
    // Healthy hypertrophy: every major anchor is present with solid share,
    // accessories sit below primaries. The card should read calm — no red.
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
                sessionCount = 14,
                topMuscles = balancedHypertrophyMuscles(),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardHypertrophyMisalignedPreview() {
    // Inverted hypertrophy: triceps sit at #1 above every primary in the
    // visible list, and quads / hams / glutes never appear. Triceps row should
    // render in semantic.error and the undercoverage callout should name the
    // missing anchors.
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
                sessionCount = 21,
                topMuscles = invertedHypertrophyMuscles(),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardMaintainPreview() {
    // Healthy maintain: balanced strength / hypertrophy split (no skew),
    // every major muscle anchor present.
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.MAINTAIN,
                strengthShare = 48,
                hypertrophyShare = 44,
                enduranceShare = 8,
                topMuscles = balancedHypertrophyMuscles(),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardMaintainMisalignedPreview() {
    // Misaligned maintain: strength dominates the declared work (>0.7 skew)
    // and the muscle list is inverted with primary anchors missing. Both the
    // WorkBalance dominant side and the muscle row should land in red.
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.MAINTAIN,
                sessionCount = 8,
                strengthShare = 80,
                hypertrophyShare = 12,
                enduranceShare = 8,
                topMuscles = invertedHypertrophyMuscles(),
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
                strengthShare = 38,
                hypertrophyShare = 30,
                enduranceShare = 32,
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardGeneralFitnessMisalignedPreview() {
    // Misaligned general fitness: strength absorbs almost all declared work,
    // endurance gets the leftovers. The dominant side label should turn red.
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.GENERAL_FITNESS,
                strengthShare = 78,
                hypertrophyShare = 12,
                enduranceShare = 10,
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalCalculationBreakdownCardLoseFatPreview() {
    PreviewContainer {
        GoalCalculationBreakdownCard(
            value = stubGoalProgress(
                primary = GoalPrimaryGoalEnumState.LOSE_FAT,
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
                topMuscles = balancedHypertrophyMuscles(),
            ),
        )
    }
}
