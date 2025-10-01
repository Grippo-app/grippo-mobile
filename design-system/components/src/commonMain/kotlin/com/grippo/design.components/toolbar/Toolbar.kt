package com.grippo.design.components.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonColorTokens
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.NavArrowLeft

@Immutable
public enum class ToolbarStyle {
    Transparent,
    Default,
}

@Composable
public fun Toolbar(
    modifier: Modifier = Modifier,
    title: String? = null,
    style: ToolbarStyle = ToolbarStyle.Default,
    onBack: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .background(
                when (style) {
                    ToolbarStyle.Transparent -> Color.Transparent
                    ToolbarStyle.Default -> AppTokens.colors.background.dialog
                }
            )
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Box(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.contentPadding.content)
                .padding(vertical = AppTokens.dp.contentPadding.content)
                .height(AppTokens.dp.screen.toolbar.height)
                .fillMaxWidth(),
        ) {
            onBack?.let {
                Button(
                    modifier = Modifier,
                    content = ButtonContent.Icon(
                        icon = AppTokens.icons.NavArrowLeft
                    ),
                    style = ButtonStyle.Custom(
                        enabled = ButtonColorTokens(
                            background = Color.Transparent,
                            content = AppTokens.colors.text.primary,
                            border = Color.Transparent,
                            icon = AppTokens.colors.icon.primary,
                        ),
                        disabled = ButtonColorTokens(
                            background = Color.Transparent,
                            content = AppTokens.colors.text.disabled,
                            border = Color.Transparent,
                            icon = AppTokens.colors.icon.disabled
                        ),
                    ),
                    size = ButtonSize.Small,
                    onClick = it
                )
            }

            title?.let {
                Text(
                    modifier = Modifier.fillMaxWidth(1f),
                    text = it,
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        content?.invoke(this)
    }
}

@AppPreview
@Composable
private fun ToolbarPreview() {
    PreviewContainer {
        Toolbar(
            title = "Secondary Secondary Secondary Secondary",
            onBack = {},
            style = ToolbarStyle.Transparent
        )
    }
}

@AppPreview
@Composable
private fun ToolbarPreviewContent() {
    PreviewContainer {
        Toolbar(
            title = "Profile",
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.Cyan)
                )
            }
        )
    }
}