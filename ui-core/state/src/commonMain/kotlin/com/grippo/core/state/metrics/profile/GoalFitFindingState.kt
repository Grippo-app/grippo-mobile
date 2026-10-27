package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.decimal_separator
import com.grippo.design.resources.provider.goal_fit_target_at_least
import com.grippo.design.resources.provider.goal_fit_target_at_most
import com.grippo.design.resources.provider.goal_fit_target_range
import com.grippo.design.resources.provider.goal_fit_target_value
import com.grippo.design.resources.provider.goal_fit_unit_groups
import com.grippo.design.resources.provider.goal_fit_unit_ratio
import com.grippo.design.resources.provider.goal_fit_unit_training_days_per_week
import com.grippo.design.resources.provider.goal_fit_unit_sets_per_week
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.grippo.toolkit.number.utils.formatDecimal

@Immutable
public data class GoalFitFindingState(
    val rule: GoalFitRuleState,
    val severity: GoalFitSeverityState,
    val actualValue: Float,
    val targetMin: Float?,
    val targetMax: Float?,
    val context: ImmutableList<MuscleGroupEnumState> = persistentListOf(),
) {
    @Composable
    public fun valueColor(): Color = severity.valueColor()

    @Composable
    public fun dotColor(): Color = severity.dotColor()

    @Composable
    public fun contextLabel(): String = context.map { it.title().text() }.joinToString(", ")

    @Composable
    public fun actualText(): String = when {
        targetMin != null && actualValue < targetMin && number(actualValue) == number(targetMin) ->
            "<${formatValue(targetMin)}"

        targetMax != null && actualValue > targetMax && number(actualValue) == number(targetMax) ->
            ">${formatValue(targetMax)}"

        else -> formatValue(actualValue)
    }

    @Composable
    public fun targetText(): String? {
        val target = when {
            targetMin != null && targetMax != null -> AppTokens.strings.res(
                Res.string.goal_fit_target_range,
                formatValue(targetMin),
                formatValue(targetMax),
            )

            targetMin != null -> AppTokens.strings.res(
                Res.string.goal_fit_target_at_least,
                formatValue(targetMin),
            )

            targetMax != null -> AppTokens.strings.res(
                Res.string.goal_fit_target_at_most,
                formatValue(targetMax),
            )

            else -> return null
        }
        return AppTokens.strings.res(Res.string.goal_fit_target_value, target)
    }

    @Composable
    private fun formatValue(value: Float): String {
        val number = number(value)
        return when (rule) {
            GoalFitRuleState.GET_STRONGER_STRENGTH_REP_RANGE_SHARE,
            GoalFitRuleState.GET_STRONGER_COMPOUND_SET_SHARE,
            GoalFitRuleState.MAINTAIN_WEEKLY_VOLUME_STABILITY,
            GoalFitRuleState.LOSE_FAT_WEEKLY_VOLUME_TREND -> "$number%"

            GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
            GoalFitRuleState.GET_STRONGER_STRENGTH_PROGRESSION ->
                if (value > 0f) "+$number%" else "$number%"

            GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
            GoalFitRuleState.LOSE_FAT_RESISTANCE_PRESENCE ->
                AppTokens.strings.res(Res.plurals.goal_fit_unit_sets_per_week, value.roundToInt(), number)

            GoalFitRuleState.BUILD_MUSCLE_WEEKLY_FREQUENCY,
            GoalFitRuleState.GET_STRONGER_WEEKLY_FREQUENCY,
            GoalFitRuleState.BUILD_MUSCLE_WEEKLY_FREQUENCY_PER_PRIMARY_GROUP,
            GoalFitRuleState.MAINTAIN_WEEKLY_FREQUENCY,
            GoalFitRuleState.GENERAL_FITNESS_WEEKLY_FREQUENCY,
            GoalFitRuleState.LOSE_FAT_SESSION_ADHERENCE,
            GoalFitRuleState.RETURN_TO_TRAINING_WEEKLY_FREQUENCY ->
                AppTokens.strings.res(Res.plurals.goal_fit_unit_training_days_per_week, value.roundToInt(), number)

            GoalFitRuleState.MAINTAIN_GROUP_COVERAGE,
            GoalFitRuleState.GENERAL_FITNESS_GROUP_COVERAGE ->
                AppTokens.strings.res(Res.plurals.goal_fit_unit_groups, value.roundToInt(), number)

            GoalFitRuleState.RETURN_TO_TRAINING_LOAD_RAMP_GENTLE ->
                "$number${AppTokens.strings.res(Res.string.goal_fit_unit_ratio)}"
        }
    }

    @Composable
    private fun number(value: Float): String {
        val scaledValue = when (rule) {
            GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
            GoalFitRuleState.GET_STRONGER_STRENGTH_PROGRESSION,
            GoalFitRuleState.MAINTAIN_WEEKLY_VOLUME_STABILITY,
            GoalFitRuleState.LOSE_FAT_WEEKLY_VOLUME_TREND -> value.toDouble() * 100.0

            else -> value.toDouble()
        }
        return formatDecimal(
            value = scaledValue,
            decimalSeparator = AppTokens.strings.res(Res.string.decimal_separator),
        )
    }
}

public fun stubGoalFitFindings(
    primary: GoalPrimaryGoalEnumState,
): ImmutableList<GoalFitFindingState> = when (primary) {
    GoalPrimaryGoalEnumState.BUILD_MUSCLE -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.PASS,
            actualValue = 3f,
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
            severity = GoalFitSeverityState.PASS,
            actualValue = 14f,
            targetMin = 10f,
            targetMax = null,
            context = persistentListOf(MuscleGroupEnumState.BACK_MUSCLES),
        ),
    )

    GoalPrimaryGoalEnumState.GET_STRONGER -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.GET_STRONGER_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.PASS,
            actualValue = 3f,
            targetMin = 2f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GET_STRONGER_STRENGTH_REP_RANGE_SHARE,
            severity = GoalFitSeverityState.WARN,
            actualValue = 28f,
            targetMin = 40f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GET_STRONGER_COMPOUND_SET_SHARE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 65f,
            targetMin = 60f,
            targetMax = null,
        ),
    )

    GoalPrimaryGoalEnumState.MAINTAIN -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.MAINTAIN_GROUP_COVERAGE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 3f,
            targetMin = 3f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.MAINTAIN_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.WARN,
            actualValue = 1.7f,
            targetMin = 2f,
            targetMax = null,
        ),
    )

    GoalPrimaryGoalEnumState.GENERAL_FITNESS -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.GENERAL_FITNESS_GROUP_COVERAGE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 3f,
            targetMin = 3f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.GENERAL_FITNESS_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.WARN,
            actualValue = 1.7f,
            targetMin = 2f,
            targetMax = null,
        ),
    )

    GoalPrimaryGoalEnumState.LOSE_FAT -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.LOSE_FAT_SESSION_ADHERENCE,
            severity = GoalFitSeverityState.PASS,
            actualValue = 2.5f,
            targetMin = 2f,
            targetMax = null,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.LOSE_FAT_RESISTANCE_PRESENCE,
            severity = GoalFitSeverityState.WARN,
            actualValue = 4f,
            targetMin = 6f,
            targetMax = null,
        ),
    )

    GoalPrimaryGoalEnumState.RETURN_TO_TRAINING -> persistentListOf(
        GoalFitFindingState(
            rule = GoalFitRuleState.RETURN_TO_TRAINING_WEEKLY_FREQUENCY,
            severity = GoalFitSeverityState.PASS,
            actualValue = 2f,
            targetMin = 1f,
            targetMax = 3f,
        ),
        GoalFitFindingState(
            rule = GoalFitRuleState.RETURN_TO_TRAINING_LOAD_RAMP_GENTLE,
            severity = GoalFitSeverityState.WARN,
            actualValue = 1.7f,
            targetMin = null,
            targetMax = 1.5f,
        ),
    )
}
