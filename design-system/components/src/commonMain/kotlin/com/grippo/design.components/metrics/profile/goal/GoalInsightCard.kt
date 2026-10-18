package com.grippo.design.components.metrics.profile.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Check
import com.grippo.design.resources.provider.icons.Info
import com.grippo.design.resources.provider.icons.Tune
import com.grippo.design.resources.provider.icons.Warning

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

    @Composable
    internal fun icon(): ImageVector = when (this) {
        Positive -> AppTokens.icons.Check
        Warning, Negative -> AppTokens.icons.Warning
        Neutral -> AppTokens.icons.Info
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTokens.colors.background.card, shape)
            .border(AppTokens.dp.metrics.panel.borderWidth, AppTokens.colors.border.default, shape)
            .padding(
                horizontal = tokens.horizontalPadding,
                vertical = tokens.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(tokens.textSpacing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Icon(
                modifier = Modifier.size(tokens.actionIcon),
                imageVector = severity.icon(),
                tint = accent,
                contentDescription = null,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = headline,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )
        }
        Text(
            text = detail,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        if (action != null) {
            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))
            ActionRow(text = action)
        }
    }
}

@Composable
private fun ActionRow(
    modifier: Modifier = Modifier,
    text: String,
) {
    val tokens = AppTokens.dp.metrics.profile.goal.insight
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Icon(
            modifier = Modifier.size(tokens.actionIcon),
            imageVector = AppTokens.icons.Tune,
            tint = AppTokens.colors.icon.secondary,
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
            detail = "Your recorded workouts match this goal's training pattern.",
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
            detail = "Your recorded sets favor higher repetition ranges than this goal's pattern.",
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
            detail = "The training pattern differs from this goal's reference range.",
            action = "Review the repetition ranges in your next workout.",
        )
    }
}

@AppPreview
@Composable
private fun GoalInsightCardNeutralPreview() {
    PreviewContainer {
        GoalInsightCard(
            severity = GoalInsightSeverity.Neutral,
            headline = "More training data needed",
            detail = "Keep recording your workouts to make the alignment estimate available.",
        )
    }
}
