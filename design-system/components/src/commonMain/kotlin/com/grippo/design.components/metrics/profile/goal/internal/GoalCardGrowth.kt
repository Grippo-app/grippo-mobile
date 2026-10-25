package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalCardStyle
import com.grippo.design.components.utils.ValueSlot
import com.grippo.design.components.utils.buildValueSlotString
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_growth_score
import com.grippo.design.resources.provider.metric_overview_goal_analyzed
import com.grippo.design.resources.provider.percent

private const val GOAL_GROWTH_DURATION_MS = 900
private const val GOAL_GROWTH_LABEL = "GoalGrowthScore"
private const val GOAL_GROWTH_ALPHA_LABEL = "GoalGrowthCurrentScore"

private val scoreRunPattern = Regex("[0-9]+|[^0-9]+")

@Composable
internal fun GoalCardGrowth(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
    previous: GoalProgressState?,
) {
    val score = value.score?.coerceIn(0, 100)
    val previousScore = previous?.score?.coerceIn(0, 100)

    var revealed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val animatedScore = animateIntAsState(
        targetValue = if (revealed || previousScore == null) score ?: 0 else previousScore,
        animationSpec = tween(durationMillis = GOAL_GROWTH_DURATION_MS, easing = FastOutSlowInEasing),
        label = GOAL_GROWTH_LABEL,
    )
    val currentScoreAlpha = animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = GOAL_GROWTH_DURATION_MS, easing = FastOutSlowInEasing),
        label = GOAL_GROWTH_ALPHA_LABEL,
    )
    val scoreColor = when {
        score == null || previousScore == null || previousScore == score -> AppTokens.colors.text.primary
        score > previousScore -> AppTokens.colors.semantic.success
        else -> AppTokens.colors.semantic.decline
    }

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        GoalIdentity(value = value)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                if (score != null) {
                    GoalScoreLine(
                        score = score,
                        previousScore = previousScore,
                        numberColor = scoreColor,
                        currentScoreAlpha = currentScoreAlpha,
                    )
                }
                Text(
                    text = value.statusLabel(),
                    style = AppTokens.typography.b12Med(),
                    color = value.statusColor(),
                )
            }

            if (score != null) {
                GoalScoreBar(
                    modifier = Modifier.fillMaxWidth(),
                    animatedScore = animatedScore,
                    colors = value.statusIndicatorColors(),
                )
            }

            Text(
                text = AppTokens.strings.res(Res.string.metric_overview_goal_analyzed, value.sessionCount),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun GoalScoreLine(
    modifier: Modifier = Modifier,
    score: Int,
    previousScore: Int?,
    numberColor: Color,
    currentScoreAlpha: State<Float>,
) {
    val percent = AppTokens.strings.res(Res.string.percent)
    val unitColor = AppTokens.colors.text.secondary
    val numberStyle = SpanStyle(color = numberColor)
    val unitStyle = AppTokens.typography.b14Med().toSpanStyle().copy(color = unitColor)

    val text = when {
        previousScore == null || previousScore == score -> scoreRuns(
            text = "$score$percent",
            numberStyle = numberStyle,
            unitStyle = unitStyle,
        )

        else -> {
            val sentence = AppTokens.strings.res(
                Res.string.goal_growth_score,
                "$previousScore$percent",
                ValueSlot.marker(0),
            )
            val alpha = currentScoreAlpha.value
            val currentNumberStyle = SpanStyle(color = numberColor.copy(alpha = numberColor.alpha * alpha))
            val currentUnitStyle = unitStyle.copy(color = unitColor.copy(alpha = unitColor.alpha * alpha))

            buildValueSlotString(
                sentence = sentence,
                slots = listOf(
                    ValueSlot(value = "$score$percent") { current ->
                        append(
                            scoreRuns(text = current, numberStyle = currentNumberStyle, unitStyle = currentUnitStyle),
                        )
                    },
                ),
                appendRest = { rest ->
                    append(scoreRuns(text = rest, numberStyle = numberStyle, unitStyle = unitStyle))
                },
            )
        }
    }

    Text(
        modifier = modifier,
        text = text,
        style = AppTokens.typography.h2(),
        color = numberColor,
    )
}

private fun scoreRuns(text: String, numberStyle: SpanStyle, unitStyle: SpanStyle): AnnotatedString {
    return buildAnnotatedString {
        scoreRunPattern.findAll(text).forEach { run ->
            val style = if (run.value.first().isDigit()) numberStyle else unitStyle
            withStyle(style) { append(run.value) }
        }
    }
}

@Composable
private fun GoalScoreBar(
    modifier: Modifier = Modifier,
    animatedScore: State<Int>,
    colors: AppColor.Charts.IndicatorColors.IndicatorColors,
) {
    LineIndicator(
        modifier = modifier,
        progress = animatedScore.value / 100f,
        colors = colors,
    )
}

@AppPreview
@Composable
private fun GoalCardGrowthImprovedPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.score == 82 }
        GoalCard(
            value = value,
            style = GoalCardStyle.Growth(previous = value.copy(score = 68, sessionCount = value.sessionCount - 1)),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardGrowthDroppedPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.score == 28 }
        GoalCard(
            value = value,
            style = GoalCardStyle.Growth(previous = value.copy(score = 45, sessionCount = value.sessionCount - 1)),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardGrowthUnchangedPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.score == 54 }
        GoalCard(
            value = value,
            style = GoalCardStyle.Growth(previous = value.copy(sessionCount = value.sessionCount - 1)),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardGrowthFirstEvaluationPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.score == 75 }
        GoalCard(
            value = value.copy(sessionCount = 1),
            style = GoalCardStyle.Growth(previous = null),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardGrowthNotEvaluatedPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first()
        GoalCard(
            value = value.copy(score = null, sessionCount = 0),
            style = GoalCardStyle.Growth(previous = value.copy(score = null, sessionCount = 0)),
        )
    }
}
