package com.grippo.design.components.example

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.design.resources.provider.icons.ArrowTop
import com.grippo.design.resources.provider.show_less
import com.grippo.design.resources.provider.show_more

@Composable
public fun ExampleDescriptionText(
    modifier: Modifier = Modifier,
    text: String,
) {
    if (text.isBlank()) return

    var expanded by remember(text) { mutableStateOf(false) }
    var isOverflow by remember(text) { mutableStateOf(false) }

    val expandLabel = AppTokens.strings.res(Res.string.show_more)
    val collapseLabel = AppTokens.strings.res(Res.string.show_less)
    val shape = RoundedCornerShape(AppTokens.dp.exerciseExampleCard.radius)

    Column(
        modifier = modifier
            .background(AppTokens.colors.background.card, shape)
            .padding(AppTokens.dp.exerciseExampleCard.contentPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Text(
            modifier = Modifier.animateContentSize(),
            text = text,
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.secondary,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) {
                    val overflowNow = result.hasVisualOverflow || result.lineCount > 3
                    if (overflowNow != isOverflow) isOverflow = overflowNow
                }
            }
        )

        if (isOverflow) {
            Button(
                modifier = Modifier.align(Alignment.End),
                content = ButtonContent.Text(
                    text = if (expanded) collapseLabel else expandLabel,
                    endIcon = ButtonIcon.Icon(
                        if (expanded) AppTokens.icons.ArrowTop else AppTokens.icons.ArrowDown
                    ),
                ),
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                onClick = { expanded = !expanded },
            )
        }
    }
}

@AppPreview
@Composable
private fun ExampleDescriptionTextLongAndShortPreview() {
    PreviewContainer {
        ExampleDescriptionText(
            modifier = Modifier.fillMaxWidth(),
            text = "Stand with your feet shoulder-width apart and hold the bar at chest height. " +
                "Keep your back neutral as you lower into a squat. Push through your feet to " +
                "return to the starting position, keeping the movement controlled throughout.",
        )

        ExampleDescriptionText(
            modifier = Modifier.fillMaxWidth(),
            text = "Keep your back neutral and move with control.",
        )
    }
}

@AppPreview
@Composable
private fun ExampleDescriptionTextEmptyPreview() {
    PreviewContainer {
        ExampleDescriptionText(
            modifier = Modifier.fillMaxWidth(),
            text = "",
        )
    }
}
