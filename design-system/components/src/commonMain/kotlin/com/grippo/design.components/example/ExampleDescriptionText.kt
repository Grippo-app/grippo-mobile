package com.grippo.design.components.example

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.show_less
import com.grippo.design.resources.provider.show_more

@Composable
public fun ExampleDescriptionText(
    modifier: Modifier = Modifier,
    text: String,
) {
    var expanded by remember { mutableStateOf(false) }
    var isOverflow by remember { mutableStateOf(false) }

    val expandLabel = AppTokens.strings.res(Res.string.show_more)
    val collapseLabel = AppTokens.strings.res(Res.string.show_less)

    Column(
        modifier = modifier
            .scalableClick(onClick = { expanded = !expanded })
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
            Text(
                modifier = Modifier.align(Alignment.End)
                    .scalableClick(onClick = { expanded = !expanded }),
                text = if (expanded) collapseLabel else expandLabel,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.tertiary,
            )
        }
    }
}

@AppPreview
@Composable
private fun ExampleDescriptionTextPreview() {
    PreviewContainer {
        ExampleDescriptionText(
            text = "Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text Text"
        )

        ExampleDescriptionText(
            text = "Text Text Text Text Text Text Text Text"
        )
    }
}