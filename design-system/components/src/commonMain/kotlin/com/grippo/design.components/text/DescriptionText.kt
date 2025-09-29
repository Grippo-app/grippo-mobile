package com.grippo.design.components.text

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.show_less
import com.grippo.design.resources.provider.show_more

@Composable
public fun DescriptionText(
    modifier: Modifier = Modifier,
    text: String,
) {
    var expanded by remember { mutableStateOf(false) }
    var isOverflow by remember { mutableStateOf(false) }


    val expandLabel = AppTokens.strings.res(Res.string.show_more)
    val collapseLabel = AppTokens.strings.res(Res.string.show_less)

    Column(
        modifier = modifier.background(
            color = AppTokens.colors.background.card,
            shape = RoundedCornerShape(AppTokens.dp.descriptionText.radius)
        ).padding(AppTokens.dp.contentPadding.content)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Text(
            text = text,
            style = AppTokens.typography.b14Med(),
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
            // Toggle control
            Button(
                modifier = Modifier.align(Alignment.End),
                content = ButtonContent.Text(
                    text = if (expanded) collapseLabel else expandLabel,
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
private fun DescriptionTextPreview() {
    PreviewContainer {
        DescriptionText(
            text = "Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text"
        )

        DescriptionText(
            text = "Text Text Text Text Text Text Text Text"
        )
    }
}