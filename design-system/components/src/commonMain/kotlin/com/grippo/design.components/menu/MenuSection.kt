package com.grippo.design.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Tune

@Immutable
public enum class MenuSectionStyle {
    Default,
    Subtle,
}

@Composable
public fun MenuSection(
    title: String,
    modifier: Modifier = Modifier,
    style: MenuSectionStyle = MenuSectionStyle.Default,
    icon: ImageVector? = null,
    iconColor: Color = AppTokens.colors.icon.secondary,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = when (style) {
                    MenuSectionStyle.Default -> AppTokens.colors.background.card
                    MenuSectionStyle.Subtle -> Color.Transparent
                },
                shape = RoundedCornerShape(AppTokens.dp.menu.radius),
            )
            .padding(AppTokens.dp.contentPadding.content),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            icon?.let {
                Icon(
                    modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                    imageVector = it,
                    tint = iconColor,
                    contentDescription = null,
                )
            }
            Text(
                text = title,
                style = AppTokens.typography.h5(),
                color = when (style) {
                    MenuSectionStyle.Default -> AppTokens.colors.text.primary
                    MenuSectionStyle.Subtle -> AppTokens.colors.text.secondary
                },
            )
        }
        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))
        content()
    }
}

@AppPreview
@Composable
private fun MenuSectionPreview() {
    PreviewContainer {
        MenuSection(title = "Body", icon = AppTokens.icons.Body, iconColor = AppTokens.colors.context.body) {
            MenuValueRow(title = "Weight", value = "82 kg", onClick = {})
            MenuValueRow(title = "Height", value = "178 cm", onClick = {}, showDivider = false)
        }
    }
}

@AppPreview
@Composable
private fun MenuSectionSubtlePreview() {
    PreviewContainer {
        MenuSection(title = "Settings", style = MenuSectionStyle.Subtle, icon = AppTokens.icons.Tune) {
            MenuValueRow(
                title = "Account",
                titleColor = AppTokens.colors.text.secondary,
                onClick = {},
            )
            MenuValueRow(
                title = "Community",
                titleColor = AppTokens.colors.text.secondary,
                onClick = {},
                showDivider = false,
            )
        }
    }
}
