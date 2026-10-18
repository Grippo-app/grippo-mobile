package com.grippo.design.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Logout
import com.grippo.design.resources.provider.icons.Trash

@Composable
public fun MenuActionRow(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    state: ButtonState = ButtonState.Enabled,
    onClick: () -> Unit,
    titleColor: Color = AppTokens.colors.text.primary,
    iconColor: Color = AppTokens.colors.icon.primary,
) {
    val enabled = state == ButtonState.Enabled
    val loading = state == ButtonState.Loading
    val resolvedTitleColor = if (state == ButtonState.Disabled) {
        AppTokens.colors.text.disabled
    } else {
        titleColor
    }
    val resolvedIconColor = if (state == ButtonState.Disabled) {
        AppTokens.colors.icon.disabled
    } else {
        iconColor
    }
    val descriptionColor = if (state == ButtonState.Disabled) {
        AppTokens.colors.text.disabled
    } else {
        AppTokens.colors.text.secondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppTokens.dp.menu.item.minHeight)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!enabled) {
                    disabled()
                }
                if (loading) {
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                }
            }
            .scalableClick(enabled = enabled, role = Role.Button, onClick = onClick)
            .clip(RectangleShape)
            .shimmer(enabled = loading)
            .padding(vertical = AppTokens.dp.menu.item.verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.menu.item.space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                tint = resolvedIconColor,
                contentDescription = null,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = title,
                style = AppTokens.typography.b14Semi(),
                color = resolvedTitleColor,
            )
            Text(
                text = description,
                style = AppTokens.typography.b12Med(),
                color = descriptionColor,
            )
        }
    }
}

@AppPreview
@Composable
private fun MenuActionRowPreview() {
    PreviewContainer {
        MenuActionRow(
            title = "Log out",
            description = "Sign out on this device. Your saved workouts stay in your account.",
            icon = AppTokens.icons.Logout,
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun MenuActionRowDestructivePreview() {
    PreviewContainer {
        MenuActionRow(
            title = "Delete account",
            description = "Permanently delete your account and training history. This cannot be undone.",
            icon = AppTokens.icons.Trash,
            onClick = {},
            titleColor = AppTokens.colors.semantic.error,
            iconColor = AppTokens.colors.semantic.error,
        )
    }
}

@AppPreview
@Composable
private fun MenuActionRowDisabledPreview() {
    PreviewContainer {
        MenuActionRow(
            title = "Delete account",
            description = "Permanently delete your account and training history. This cannot be undone.",
            icon = AppTokens.icons.Trash,
            state = ButtonState.Disabled,
            onClick = {},
            titleColor = AppTokens.colors.semantic.error,
            iconColor = AppTokens.colors.semantic.error,
        )
    }
}

@AppPreview
@Composable
private fun MenuActionRowLoadingPreview() {
    PreviewContainer {
        MenuActionRow(
            title = "Log out",
            description = "Sign out on this device. Your saved workouts stay in your account.",
            icon = AppTokens.icons.Logout,
            state = ButtonState.Loading,
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun MenuActionRowLongContentPreview() {
    PreviewContainer {
        MenuActionRow(
            title = "Вийти з облікового запису на цьому пристрої",
            description = "Ви вийдете з облікового запису лише на цьому пристрої. Усі збережені тренування залишаться у вашому обліковому записі.",
            icon = AppTokens.icons.Logout,
            onClick = {},
        )
    }
}
