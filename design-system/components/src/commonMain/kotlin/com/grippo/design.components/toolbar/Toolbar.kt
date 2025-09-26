package com.grippo.design.components.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.Side
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.NavArrowLeft
import kotlinx.collections.immutable.persistentListOf

@Immutable
public enum class ToolbarStyle {
    Default,
    Transparent
}

@Composable
public fun Toolbar(
    modifier: Modifier = Modifier,
    title: String? = null,
    style: ToolbarStyle = ToolbarStyle.Default,
    onBack: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {

    val color = when (style) {
        ToolbarStyle.Default -> AppTokens.colors.background.dialog
        ToolbarStyle.Transparent -> Color.Transparent
    }

    val shadowElevation = when (style) {
        ToolbarStyle.Default -> ShadowElevation.Container
        ToolbarStyle.Transparent -> ShadowElevation.Non
    }

    Column(
        modifier = modifier
            .shadowDefault(
                shape = RoundedCornerShape(0.dp),
                elevation = shadowElevation,
                sides = persistentListOf(Side.BOTTOM)
            )
            .background(color)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.screen.toolbar.height),
        ) {
            onBack?.let {
                Icon(
                    modifier = Modifier
                        .scalableClick(onClick = it)
                        .fillMaxHeight()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                    imageVector = AppTokens.icons.NavArrowLeft,
                    contentDescription = null,
                    tint = AppTokens.colors.icon.primary,

                    )
            }

            title?.let {
                Text(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.screen.toolbar.height)
                        .align(Alignment.Center),
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
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
            title = "Primary Primary Primary Primary Primary",
            onBack = {},
            style = ToolbarStyle.Default
        )

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