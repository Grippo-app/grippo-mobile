package com.grippo.design.components.metrics.profile

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

@Immutable
public enum class ProfileInsightSeverity {
    Positive,
    Warning,
    Negative,
    Neutral;

    @Composable
    internal fun color(): Color = when (this) {
        Positive -> AppTokens.colors.semantic.success
        Warning -> AppTokens.colors.semantic.warning
        Negative -> AppTokens.colors.semantic.error
        Neutral -> AppTokens.colors.semantic.info
    }
}

@Composable
public fun ProfileInsightCard(
    severity: ProfileInsightSeverity,
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
                        text = action,
                        style = AppTokens.typography.b13Semi(),
                        color = AppTokens.colors.text.primary,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ProfileInsightCardPositivePreview() {
    PreviewContainer {
        ProfileInsightCard(
            severity = ProfileInsightSeverity.Positive,
            headline = "Compound foundation",
            detail = "Most of the load came from multi-joint lifts — efficient and easy to progress.",
        )
    }
}

@AppPreview
@Composable
private fun ProfileInsightCardWarningPreview() {
    PreviewContainer {
        ProfileInsightCard(
            severity = ProfileInsightSeverity.Warning,
            headline = "Push/Pull is uneven",
            detail = "One direction did most of the work this period.",
            action = "Add a pulling movement next session — row or pull-up.",
        )
    }
}

@AppPreview
@Composable
private fun ProfileInsightCardNegativePreview() {
    PreviewContainer {
        ProfileInsightCard(
            severity = ProfileInsightSeverity.Negative,
            headline = "Easy session",
            detail = "Not enough working stimulus to classify a style today.",
            action = "Add 1 hard set or push closer to failure.",
        )
    }
}

@AppPreview
@Composable
private fun ProfileInsightCardNeutralPreview() {
    PreviewContainer {
        ProfileInsightCard(
            severity = ProfileInsightSeverity.Neutral,
            headline = "No clear style",
            detail = "Strength, hypertrophy and endurance all showed up.",
            action = "Pick one knob next session: heavier, more sets, or shorter rests.",
        )
    }
}
