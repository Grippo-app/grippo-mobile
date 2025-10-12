package com.grippo.design.components.ai

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Cancel
import kotlinx.coroutines.delay

@Composable
public fun AiSuggestionCard(
    modifier: Modifier = Modifier,
    value: String,
    onClose: () -> Unit
) {
    // Typewriter state
    var displayedText by remember { mutableStateOf("") }

    // Restart typing when 'value' changes
    LaunchedEffect(value) {
        displayedText = ""
        // Tune typing speed if needed
        val typingSpeedMs = 18L
        for (i in value.indices) {
            displayedText = value.substring(0, i + 1)
            delay(typingSpeedMs)
        }
    }

    Row(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    0f to AppTokens.colors.aiSuggestion.background1,
                    1f to AppTokens.colors.aiSuggestion.background2,
                ),
                shape = RoundedCornerShape(AppTokens.dp.aiSuggestionCard.radius)
            ).border(
                width = 2.dp,
                shape = RoundedCornerShape(AppTokens.dp.aiSuggestionCard.radius),
                color = AppTokens.colors.aiSuggestion.border
            ).padding(
                vertical = AppTokens.dp.aiSuggestionCard.verticalPadding,
                horizontal = AppTokens.dp.aiSuggestionCard.horizontalPadding
            ).animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = displayedText,
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.aiSuggestion.content,
            softWrap = true
        )

        Button(
            content = ButtonContent.Icon(
                icon = AppTokens.icons.Cancel,
            ),
            style = ButtonStyle.Transparent,
            size = ButtonSize.Small,
            onClick = onClose
        )
    }
}

@Composable
@AppPreview
private fun AiSuggestionCardPreview() {
    PreviewContainer {
        AiSuggestionCard(
            value = "For some reason",
            onClose = {}
        )
    }
}