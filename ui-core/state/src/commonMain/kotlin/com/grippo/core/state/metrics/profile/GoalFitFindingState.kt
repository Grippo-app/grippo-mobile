package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_fit_inversion_inverted
import com.grippo.design.resources.provider.goal_fit_inversion_none
import com.grippo.design.resources.provider.goal_fit_target_at_least
import com.grippo.design.resources.provider.goal_fit_target_at_most
import com.grippo.design.resources.provider.goal_fit_target_count_of
import com.grippo.design.resources.provider.goal_fit_target_range
import com.grippo.design.resources.provider.goal_fit_unit_groups
import com.grippo.design.resources.provider.goal_fit_unit_lifts
import com.grippo.design.resources.provider.goal_fit_unit_qualities
import com.grippo.design.resources.provider.goal_fit_unit_ratio
import com.grippo.design.resources.provider.goal_fit_unit_sessions_per_week
import com.grippo.design.resources.provider.goal_fit_unit_sets_per_week
import com.grippo.design.resources.provider.goal_fit_value_target
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
public data class GoalFitFindingState(
    val rule: GoalFitRuleState,
    val severity: GoalFitSeverityState,
    val actualValue: Float,
    val targetMin: Float?,
    val targetMax: Float?,
    val context: ImmutableList<String> = persistentListOf(),
) {
    @Composable
    public fun valueColor(): Color = severity.valueColor()

    @Composable
    public fun dotColor(): Color = severity.dotColor()

    /** Combined "actual · target X" line for the row's right column. */
    @Composable
    public fun valueTargetText(): String {
        val (actual, target) = formatActualAndTarget()
        return AppTokens.strings.res(Res.string.goal_fit_value_target, actual, target)
    }

    /** Subline for the inversion rule — PASS shows neutral text, FAIL lists groups. */
    @Composable
    public fun inversionStatusText(): String {
        if (severity == GoalFitSeverityState.PASS || context.isEmpty()) {
            return AppTokens.strings.res(Res.string.goal_fit_inversion_none)
        }
        return AppTokens.strings.res(Res.string.goal_fit_inversion_inverted, joinContextLabels())
    }

    /** Human label for [context] entries — resolves muscle-group enum names through state titles. */
    @Composable
    public fun contextLabel(): String = joinContextLabels()

    @Composable
    private fun joinContextLabels(): String {
        if (context.isEmpty()) return ""
        val labels = ArrayList<String>(context.size)
        for (raw in context) {
            val group = MuscleGroupEnumState.entries.firstOrNull { it.name == raw }
            labels.add(group?.title()?.text() ?: raw)
        }
        return labels.joinToString(separator = ", ")
    }

    /**
     * Per-rule formatter. Single `when` over the rule enum — every PASS-window
     * shape (one-sided, two-sided, signed, count-of-total, ratio) is expanded
     * inline so the @Composable call graph stays flat (one helper instead of
     * the ten-deep tree the previous split produced).
     */
    @Composable
    private fun formatActualAndTarget(): Pair<String, String> = when (rule) {

        // Percent share — PASS at-least.
        GoalFitRuleState.BUILD_MUSCLE_HYPERTROPHY_REP_RANGE_SHARE,
        GoalFitRuleState.GET_STRONGER_HEAVY_INTENSITY_SHARE,
        GoalFitRuleState.GET_STRONGER_STRENGTH_REP_RANGE_SHARE -> {
            val actual = formatIntPercent(actualValue)
            val target = if (targetMin != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_least,
                    formatIntPercent(targetMin)
                )
            } else ""
            actual to target
        }

        // Percent share — PASS at-most.
        GoalFitRuleState.RETURN_TO_TRAINING_INTENSITY_FLOOR -> {
            val actual = formatIntPercent(actualValue)
            val target = if (targetMax != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_most,
                    formatIntPercent(targetMax)
                )
            } else ""
            actual to target
        }

        // Fraction-as-percent — PASS at-most (skew, stability).
        GoalFitRuleState.MAINTAIN_WORK_BALANCE_SKEW,
        GoalFitRuleState.GENERAL_FITNESS_WORK_BALANCE_SKEW,
        GoalFitRuleState.MAINTAIN_WEEKLY_VOLUME_STABILITY -> {
            val actual = formatIntPercent(actualValue * 100f)
            val target = if (targetMax != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_most,
                    formatIntPercent(targetMax * 100f)
                )
            } else ""
            actual to target
        }

        // Fraction-as-percent — PASS at-least (volume trend).
        GoalFitRuleState.LOSE_FAT_WEEKLY_VOLUME_TREND -> {
            val actual = formatIntPercent(actualValue * 100f)
            val target = if (targetMin != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_least,
                    formatIntPercent(targetMin * 100f)
                )
            } else ""
            actual to target
        }

        // Signed-percent trend (load / strength progression).
        GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
        GoalFitRuleState.GET_STRONGER_STRENGTH_PROGRESSION -> {
            val actual = formatSignedPercent(actualValue * 100f)
            val target = if (targetMin != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_least,
                    formatSignedPercent(targetMin * 100f)
                )
            } else ""
            actual to target
        }

        // Sets/week — PASS in [min, max] window.
        GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP -> {
            val unit = AppTokens.strings.res(Res.string.goal_fit_unit_sets_per_week)
            val actual = "${formatInt(actualValue)} $unit"
            val target = if (targetMin != null && targetMax != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_range,
                    formatInt(targetMin),
                    formatInt(targetMax)
                )
            } else ""
            actual to target
        }

        // Sets/week — PASS at-least.
        GoalFitRuleState.LOSE_FAT_RESISTANCE_PRESENCE -> {
            val unit = AppTokens.strings.res(Res.string.goal_fit_unit_sets_per_week)
            val actual = "${formatInt(actualValue)} $unit"
            val target = if (targetMin != null) {
                AppTokens.strings.res(Res.string.goal_fit_target_at_least, formatInt(targetMin))
            } else ""
            actual to target
        }

        // Sessions/week — PASS at-least.
        GoalFitRuleState.MAINTAIN_WEEKLY_FREQUENCY,
        GoalFitRuleState.GENERAL_FITNESS_WEEKLY_FREQUENCY,
        GoalFitRuleState.LOSE_FAT_SESSION_ADHERENCE,
        GoalFitRuleState.BUILD_MUSCLE_WEEKLY_FREQUENCY_PER_PRIMARY_GROUP -> {
            val unit = AppTokens.strings.res(Res.string.goal_fit_unit_sessions_per_week)
            val actual = "${formatDecimal1(actualValue)} $unit"
            val target = if (targetMin != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_least,
                    formatDecimal1(targetMin)
                )
            } else ""
            actual to target
        }

        // Sessions/week — PASS in [min, max] window.
        GoalFitRuleState.RETURN_TO_TRAINING_WEEKLY_FREQUENCY -> {
            val unit = AppTokens.strings.res(Res.string.goal_fit_unit_sessions_per_week)
            val actual = "${formatDecimal1(actualValue)} $unit"
            val target = if (targetMin != null && targetMax != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_range,
                    formatDecimal1(targetMin),
                    formatDecimal1(targetMax),
                )
            } else ""
            actual to target
        }

        // Count-of-total — "X of Y unit". Unit varies per rule.
        GoalFitRuleState.GET_STRONGER_COMPOUND_PRESENCE,
        GoalFitRuleState.MAINTAIN_GROUP_COVERAGE,
        GoalFitRuleState.GENERAL_FITNESS_GROUP_COVERAGE,
        GoalFitRuleState.GENERAL_FITNESS_QUALITY_VARIETY -> {
            val unit = AppTokens.strings.res(
                when (rule) {
                    GoalFitRuleState.GET_STRONGER_COMPOUND_PRESENCE -> Res.string.goal_fit_unit_lifts
                    GoalFitRuleState.GENERAL_FITNESS_QUALITY_VARIETY -> Res.string.goal_fit_unit_qualities
                    else -> Res.string.goal_fit_unit_groups
                }
            )
            val totalText = formatInt(targetMax ?: 0f)
            val actualOf = AppTokens.strings.res(
                Res.string.goal_fit_target_count_of,
                formatInt(actualValue),
                totalText,
            )
            val actual = "$actualOf $unit"
            val target = if (targetMin != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_least,
                    AppTokens.strings.res(
                        Res.string.goal_fit_target_count_of,
                        formatInt(targetMin),
                        totalText
                    ),
                )
            } else ""
            actual to target
        }

        // Ratio — PASS at-least.
        GoalFitRuleState.GET_STRONGER_COMPOUND_TO_ISOLATION_RATIO -> {
            val unit = AppTokens.strings.res(Res.string.goal_fit_unit_ratio)
            val actual = "${formatDecimal1(actualValue)}$unit"
            val target = if (targetMin != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_least,
                    "${formatDecimal1(targetMin)}$unit",
                )
            } else ""
            actual to target
        }

        // Ratio — PASS at-most.
        GoalFitRuleState.RETURN_TO_TRAINING_LOAD_RAMP_GENTLE -> {
            val unit = AppTokens.strings.res(Res.string.goal_fit_unit_ratio)
            val actual = "${formatDecimal1(actualValue)}$unit"
            val target = if (targetMax != null) {
                AppTokens.strings.res(
                    Res.string.goal_fit_target_at_most,
                    "${formatDecimal1(targetMax)}$unit",
                )
            } else ""
            actual to target
        }

        // Inversion has its own renderer and never goes through value/target text.
        GoalFitRuleState.BUILD_MUSCLE_ACCESSORY_INVERSION -> "" to ""
    }

    // ---- Pure number formatters — KMP-friendly (no String.format dependency). ----

    private fun formatInt(value: Float): String = value.roundToInt().toString()

    private fun formatIntPercent(value: Float): String =
        "${value.coerceIn(-PERCENT_CAP, PERCENT_CAP).roundToInt()}%"

    private fun formatSignedPercent(value: Float): String {
        val rounded = value.coerceIn(-PERCENT_CAP, PERCENT_CAP).roundToInt()
        val sign = when {
            rounded > 0 -> "+"
            rounded < 0 -> "−"
            else -> ""
        }
        return "$sign${abs(rounded)}%"
    }

    private fun formatDecimal1(value: Float): String {
        val whole = value.toInt()
        val tenths = ((abs(value) - abs(whole)) * 10f).roundToInt().coerceIn(0, 9)
        val sign = if (value < 0f && whole == 0) "−" else ""
        return "$sign$whole.$tenths"
    }

    private companion object {
        /** Cap for percent formatters — keeps 4-digit garbage out of the row text. */
        private const val PERCENT_CAP: Float = 1000f
    }
}

/** Per-goal stub used by previews. Mirrors the shape the evaluator emits in production. */
public fun stubGoalFitFindings(
    primary: GoalPrimaryGoalEnumState,
): ImmutableList<GoalFitFindingState> = when (primary) {
    GoalPrimaryGoalEnumState.BUILD_MUSCLE -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_HYPERTROPHY_REP_RANGE_SHARE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 68f,
            targetMin = 60f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
            severity = GoalFitSeverityState.PASS,
            actualValue = 14f,
            targetMin = 10f,
            targetMax = 20f,
            context = persistentListOf("CHEST_MUSCLES"),
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
            severity = GoalFitSeverityState.WARN,
            actualValue = 8f,
            targetMin = 10f,
            targetMax = 20f,
            context = persistentListOf("BACK_MUSCLES"),
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
            severity = GoalFitSeverityState.FAIL,
            actualValue = 3f,
            targetMin = 10f,
            targetMax = 20f,
            context = persistentListOf("LEGS"),
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_ACCESSORY_INVERSION,
            severity = GoalFitSeverityState.PASS,
            actualValue = 0f,
            targetMin = null,
            targetMax = 0f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
            severity = GoalFitSeverityState.PASS,
            actualValue = 0.04f,
            targetMin = 0.01f,
            targetMax = null,
        ),
    )

    GoalPrimaryGoalEnumState.GET_STRONGER -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.GET_STRONGER_COMPOUND_PRESENCE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 4f,
            targetMin = 3f,
            targetMax = 5f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GET_STRONGER_HEAVY_INTENSITY_SHARE,
            severity = GoalFitSeverityState.WARN,
            actualValue = 28f,
            targetMin = 40f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GET_STRONGER_STRENGTH_REP_RANGE_SHARE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 58f,
            targetMin = 50f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GET_STRONGER_COMPOUND_TO_ISOLATION_RATIO,
            severity = GoalFitSeverityState.PASS,
            actualValue = 2.4f,
            targetMin = 2.0f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GET_STRONGER_STRENGTH_PROGRESSION,
            severity = GoalFitSeverityState.FAIL,
            actualValue = -0.02f,
            targetMin = 0.03f,
            targetMax = null,
        ),
    )

    GoalPrimaryGoalEnumState.MAINTAIN -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.MAINTAIN_WORK_BALANCE_SKEW,
            severity = GoalFitSeverityState.PASS,
            actualValue = 0.55f,
            targetMin = null,
            targetMax = 0.70f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.MAINTAIN_GROUP_COVERAGE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 6f,
            targetMin = 5f,
            targetMax = 6f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.MAINTAIN_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.WARN,
            actualValue = 1.7f,
            targetMin = 2.0f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.MAINTAIN_WEEKLY_VOLUME_STABILITY,
            severity = GoalFitSeverityState.FAIL,
            actualValue = 0.72f,
            targetMin = null,
            targetMax = 0.30f,
        ),
    )

    GoalPrimaryGoalEnumState.GENERAL_FITNESS -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.GENERAL_FITNESS_QUALITY_VARIETY,
            severity = GoalFitSeverityState.PASS,
            actualValue = 3f,
            targetMin = 3f,
            targetMax = 3f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GENERAL_FITNESS_GROUP_COVERAGE,
            severity = GoalFitSeverityState.WARN,
            actualValue = 4f,
            targetMin = 5f,
            targetMax = 6f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GENERAL_FITNESS_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.PASS,
            actualValue = 2.5f,
            targetMin = 2.0f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GENERAL_FITNESS_WORK_BALANCE_SKEW,
            severity = GoalFitSeverityState.FAIL,
            actualValue = 0.91f,
            targetMin = null,
            targetMax = 0.70f,
        ),
    )

    GoalPrimaryGoalEnumState.LOSE_FAT -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.LOSE_FAT_SESSION_ADHERENCE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 3.4f,
            targetMin = 3.0f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.LOSE_FAT_RESISTANCE_PRESENCE,
            severity = GoalFitSeverityState.WARN,
            actualValue = 4f,
            targetMin = 6f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.LOSE_FAT_WEEKLY_VOLUME_TREND,
            severity = GoalFitSeverityState.FAIL,
            actualValue = 0.55f,
            targetMin = 0.85f,
            targetMax = null,
        ),
    )

    GoalPrimaryGoalEnumState.RETURN_TO_TRAINING -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.RETURN_TO_TRAINING_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.PASS,
            actualValue = 2.0f,
            targetMin = 1.0f,
            targetMax = 3.0f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.RETURN_TO_TRAINING_LOAD_RAMP_GENTLE,
            severity = GoalFitSeverityState.WARN,
            actualValue = 1.7f,
            targetMin = null,
            targetMax = 1.5f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.RETURN_TO_TRAINING_INTENSITY_FLOOR,
            severity = GoalFitSeverityState.FAIL,
            actualValue = 32f,
            targetMin = null,
            targetMax = 10f,
        ),
    )
}
