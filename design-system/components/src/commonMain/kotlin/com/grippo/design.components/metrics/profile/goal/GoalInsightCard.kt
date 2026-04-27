package com.grippo.design.components.metrics.profile.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowRight

/**
 * Severity of a goal insight row. Drives the accent stripe color so users
 * can scan a list of insights and immediately see which are good / off.
 */
@Immutable
public enum class GoalInsightSeverity {
    /** Something is working well - green. */
    Positive,

    /** Something is drifting - amber. */
    Warning,

    /** Something is clearly off - red. */
    Negative,

    /** Neutral, informational - muted. */
    Neutral;

    @Composable
    internal fun color(): Color = when (this) {
        Positive -> AppTokens.colors.semantic.success
        Warning -> AppTokens.colors.semantic.warning
        Negative -> AppTokens.colors.semantic.error
        Neutral -> AppTokens.colors.semantic.info
    }
}

/**
 * A single diagnostic row about the goal: a colored vertical accent stripe
 * communicating severity at a glance, followed by a headline, an
 * explanation, and an optional action — the next step paired to this signal.
 *
 * The action line shares the severity accent so a user can scan a
 * red insight and immediately see the red-accented action that pairs to it.
 *
 * All dimensions come from `AppTokens.dp.metrics.profile.goal.insight` so the card
 * stays consistent with the rest of the goal widgets.
 */
@Composable
public fun GoalInsightCard(
    severity: GoalInsightSeverity,
    headline: String,
    detail: String,
    modifier: Modifier = Modifier,
    action: String? = null,
) {
    val accent = severity.color()
    val tokens = AppTokens.dp.metrics.profile.goal.insight
    val shape = RoundedCornerShape(tokens.radius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTokens.colors.background.card, shape)
            .height(intrinsicSize = IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(tokens.accentWidth)
                .background(accent.copy(alpha = 0.7f)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = tokens.horizontalPadding,
                    vertical = tokens.verticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(tokens.textSpacing),
        ) {
            Text(
                text = headline,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = detail,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )

            if (action != null) {
                Spacer(Modifier.height(AppTokens.dp.contentPadding.text))
                ActionRow(text = action, accent = accent)
            }
        }
    }
}

@Composable
private fun ActionRow(text: String, accent: Color) {
    val tokens = AppTokens.dp.metrics.profile.goal.insight
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Icon(
            modifier = Modifier.size(tokens.actionIcon),
            imageVector = AppTokens.icons.ArrowRight,
            tint = accent,
            contentDescription = null,
        )
        Text(
            text = text,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@AppPreview
@Composable
private fun GoalInsightCardPositivePreview() {
    PreviewContainer {
        GoalInsightCard(
            severity = GoalInsightSeverity.Positive,
            headline = "On track",
            detail = "You're hitting your weekly target consistently. Keep it up.",
        )
    }
}

@AppPreview
@Composable
private fun GoalInsightCardWarningPreview() {
    PreviewContainer {
        GoalInsightCard(
            severity = GoalInsightSeverity.Warning,
            headline = "Slightly drifting",
            detail = "Your pace has slowed over the last few sessions.",
        )
    }
}

@AppPreview
@Composable
private fun GoalInsightCardNegativePreview() {
    PreviewContainer {
        GoalInsightCard(
            severity = GoalInsightSeverity.Negative,
            headline = "Off track",
            detail = "Progress has stalled. Time to adjust the plan.",
            action = "Add 1–2 heavy compound lifts (3–5 reps) per session.",
        )
    }
}

@AppPreview
@Composable
private fun GoalInsightCardNeutralPreview() {
    PreviewContainer {
        GoalInsightCard(
            severity = GoalInsightSeverity.Neutral,
            headline = "Almost done",
            detail = "You're close to the finish — stay consistent.",
        )
    }
}
