package com.grippo.design.components.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.NavArrowLeft
import com.grippo.design.resources.provider.icons.User

@Immutable
public enum class ToolbarStyle {
    Transparent,
    Default,
}

@Immutable
public sealed class Leading {
    public data class Back(
        val onClick: (() -> Unit)
    ) : Leading()

    public data class Profile(
        val onClick: (() -> Unit)
    ) : Leading()

    public data object Nothing : Leading()
}

@Composable
public fun Toolbar(
    modifier: Modifier = Modifier,
    title: String? = null,
    style: ToolbarStyle = ToolbarStyle.Default,
    leading: Leading = Leading.Nothing,
    trailing: (@Composable BoxScope.() -> Unit)? = null,
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

            when (leading) {
                is Leading.Back -> Button(
                    modifier = Modifier,
                    content = ButtonContent.Icon(
                        icon = AppTokens.icons.NavArrowLeft
                    ),
                    style = ButtonStyle.Transparent,
                    size = ButtonSize.Small,
                    onClick = leading.onClick
                )

                is Leading.Profile -> Button(
                    modifier = Modifier,
                    content = ButtonContent.Icon(
                        icon = AppTokens.icons.User
                    ),
                    style = ButtonStyle.Transparent,
                    size = ButtonSize.Small,
                    onClick = leading.onClick
                )

                Leading.Nothing -> {
                    // Skip
                }
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

            trailing?.let { trailing ->
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    content = trailing,
                    contentAlignment = Alignment.Center
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
            leading = Leading.Nothing,
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