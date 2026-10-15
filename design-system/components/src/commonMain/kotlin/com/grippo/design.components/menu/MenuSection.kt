package com.grippo.design.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    modifier: Modifier = Modifier,
    title: String,
    style: MenuSectionStyle = MenuSectionStyle.Default,
    icon: ImageVector? = null,
    iconColor: Color = AppTokens.colors.icon.secondary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AppTokens.dp.menu.radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = when (style) {
                    MenuSectionStyle.Default -> AppTokens.colors.background.card
                    MenuSectionStyle.Subtle -> Color.Transparent
                },
                shape = shape,
            )
            .border(
                width = AppTokens.dp.menu.borderWidth,
                color = when (style) {
                    MenuSectionStyle.Default -> AppTokens.colors.border.default
                    MenuSectionStyle.Subtle -> Color.Transparent
                },
                shape = shape,
            )
            .padding(AppTokens.dp.menu.contentPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(AppTokens.dp.menu.headerIconContainer)
                        .background(
                            color = when (style) {
                                MenuSectionStyle.Default -> AppTokens.colors.background.screen
                                MenuSectionStyle.Subtle -> AppTokens.colors.background.card
                            },
                            shape = RoundedCornerShape(AppTokens.dp.menu.headerIconRadius),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(AppTokens.dp.menu.headerIcon),
                        imageVector = it,
                        tint = iconColor,
                        contentDescription = null,
                    )
                }
            }
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = AppTokens.typography.h4(),
                color = when (style) {
                    MenuSectionStyle.Default -> AppTokens.colors.text.primary
                    MenuSectionStyle.Subtle -> AppTokens.colors.text.secondary
                },
            )
        }
        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
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

@AppPreview
@Composable
private fun MenuSectionLoadingPreview() {
    PreviewContainer {
        MenuSection(title = "Body", icon = AppTokens.icons.Body, iconColor = AppTokens.colors.context.body) {
            MenuValueRow(title = "Weight", loading = true, onClick = {})
            MenuValueRow(title = "Height", value = "Not set", onClick = {}, showDivider = false)
        }
    }
}

@AppPreview
@Composable
private fun MenuSectionLongContentPreview() {
    PreviewContainer {
        MenuSectionStyle.entries.forEach { style ->
            MenuSection(
                title = "Налаштування та обмеження тренувань",
                style = style,
                icon = AppTokens.icons.Tune,
            ) {
                MenuValueRow(
                    title = "Рівень тренувального досвіду",
                    value = "Досвідчений спортсмен",
                    onClick = {},
                )
                MenuValueRow(
                    title = "Спільнота та підтримка",
                    onClick = {},
                    showDivider = false,
                )
            }
        }
    }
}
