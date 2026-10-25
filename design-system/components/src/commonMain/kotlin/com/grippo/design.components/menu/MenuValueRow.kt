package com.grippo.design.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Edit
import com.grippo.design.resources.provider.icons.Info
import com.grippo.design.resources.provider.icons.Instagram
import com.grippo.design.resources.provider.icons.Threads
import com.grippo.design.resources.provider.icons.Trash
import com.grippo.design.resources.provider.loading

@Composable
public fun MenuValueRow(
    modifier: Modifier = Modifier,
    title: String,
    value: String? = null,
    icon: ImageVector? = null,
    loading: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit,
    titleColor: Color = AppTokens.colors.text.primary,
    iconColor: Color = AppTokens.colors.icon.primary,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val valueMaxWidth = maxWidth * AppTokens.dp.menu.item.valueMaxWidthFraction

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppTokens.dp.menu.item.minHeight)
                    .scalableClick(role = Role.Button, onClick = onClick)
                    .padding(vertical = AppTokens.dp.menu.item.verticalPadding),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.menu.item.space),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(AppTokens.dp.menu.item.iconContainer)
                            .background(
                                color = AppTokens.colors.background.card,
                                shape = RoundedCornerShape(AppTokens.dp.menu.item.iconRadius),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                            imageVector = icon,
                            tint = iconColor,
                            contentDescription = null,
                        )
                    }
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = AppTokens.typography.b14Semi(),
                    color = titleColor,
                )
                MenuRowValue(
                    modifier = Modifier.widthIn(max = valueMaxWidth),
                    value = value,
                    loading = loading,
                )
                Icon(
                    modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                    imageVector = AppTokens.icons.ArrowRight,
                    tint = AppTokens.colors.icon.secondary,
                    contentDescription = null,
                )
            }
        }
        if (showDivider) {
            ContentDivider(
                modifier = if (icon == null) {
                    Modifier
                } else {
                    Modifier.padding(
                        start = AppTokens.dp.menu.item.iconContainer + AppTokens.dp.menu.item.space,
                    )
                },
            )
        }
    }
}

@Composable
private fun MenuRowValue(
    modifier: Modifier = Modifier,
    value: String?,
    loading: Boolean,
) {
    val style = AppTokens.typography.b14Med()

    when {
        loading -> {
            val loadingDescription = AppTokens.strings.res(Res.string.loading)

            SkeletonText(
                modifier = modifier
                    .fillMaxWidth(AppTokens.dp.skeleton.line.short)
                    .semantics {
                        contentDescription = loadingDescription
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    },
                style = style,
                animated = true,
            )
        }

        value != null -> {
            Text(
                modifier = modifier,
                text = value,
                style = style,
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@AppPreview
@Composable
private fun MenuValueRowPreview() {
    PreviewContainer {
        MenuSection(
            title = "Body",
            icon = AppTokens.icons.Body,
            iconColor = AppTokens.colors.context.body,
        ) {
            MenuValueRow(title = "Weight", value = "82 kg", onClick = {})
            MenuValueRow(title = "Height", value = "178 cm", loading = true, onClick = {})
            MenuValueRow(title = "Experience", value = "—", onClick = {}, showDivider = false)
        }
    }
}

@AppPreview
@Composable
private fun MenuValueRowSettingsPreview() {
    PreviewContainer {
        MenuSection(title = "Settings", style = MenuSectionStyle.Subtle) {
            MenuValueRow(title = "Account", titleColor = AppTokens.colors.text.secondary, onClick = {})
            MenuValueRow(title = "Community", titleColor = AppTokens.colors.text.secondary, onClick = {})
            MenuValueRow(
                title = "Debug",
                titleColor = AppTokens.colors.semantic.error,
                onClick = {},
                showDivider = false,
            )
        }
    }
}

@AppPreview
@Composable
private fun MenuValueRowSocialPreview() {
    PreviewContainer {
        Column {
            MenuValueRow(
                title = "Instagram",
                icon = AppTokens.icons.Instagram,
                iconColor = AppTokens.colors.brand.color1,
                onClick = {},
            )
            MenuValueRow(
                title = "Threads",
                icon = AppTokens.icons.Threads,
                iconColor = AppTokens.colors.text.primary,
                onClick = {},
                showDivider = false,
            )
        }
    }
}

@AppPreview
@Composable
private fun MenuValueRowDialogPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier
                .background(AppTokens.colors.background.dialog)
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        ) {
            MenuValueRow(
                title = "Details",
                icon = AppTokens.icons.Info,
                iconColor = AppTokens.colors.brand.color5,
                onClick = {},
            )
            MenuValueRow(
                title = "Edit",
                icon = AppTokens.icons.Edit,
                iconColor = AppTokens.colors.brand.color2,
                onClick = {},
            )
            MenuValueRow(
                title = "Delete",
                icon = AppTokens.icons.Trash,
                titleColor = AppTokens.colors.semantic.error,
                iconColor = AppTokens.colors.semantic.error,
                onClick = {},
                showDivider = false,
            )
        }
    }
}

@AppPreview
@Composable
private fun MenuValueRowLongContentPreview() {
    PreviewContainer {
        MenuSection(title = "Тренувальні налаштування") {
            MenuValueRow(
                title = "Основна ціль тренувань",
                value = "Нарощування м’язової маси",
                onClick = {},
            )
            MenuValueRow(title = "Виключене обладнання", value = "0", onClick = {})
            MenuValueRow(
                title = "Досвід тренувань",
                value = "Не вказано",
                onClick = {},
                showDivider = false,
            )
        }
    }
}
