package com.grippo.design.components.metrics.profile.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        Neutral -> AppTokens.colors.text.tertiary
    }
}

@Composable
public fun GoalInsightCard(
    modifier: Modifier = Modifier,
    severity: GoalInsightSeverity,
    headline: String,
    detail: String,
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
            .border(AppTokens.dp.metrics.panel.borderWidth, AppTokens.colors.border.default, shape)
            .height(intrinsicSize = IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(AppTokens.dp.accent.smallWidth)
                .background(accent),
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
                style = AppTokens.typography.h5(),
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
private fun ActionRow(
    modifier: Modifier = Modifier,
    text: String,
    accent: Color,
) {
    val tokens = AppTokens.dp.metrics.profile.goal.insight
    Row(
        modifier = modifier.fillMaxWidth(),
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
