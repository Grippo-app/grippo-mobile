package com.grippo.profile.menu.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.core.state.profile.RoleEnumState
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ProfileMenuContent(
    role: RoleEnumState?,
    onProfileMenuClick: (ProfileMenu) -> Unit,
    onSettingsMenuClick: (SettingsMenu) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileMenu = ProfileMenu.entries
        .map {
            it to MenuItem(
                title = it.text(),
                icon = it.icon(),
                titleColor = it.textColor(),
                iconColor = it.iconColor()
            )
        }.toPersistentList()

    val settingsMenu = SettingsMenu.entries
        .filter { it != SettingsMenu.Debug || role == RoleEnumState.ADMIN }
        .map {
            it to MenuItem(
                title = it.text(),
                icon = it.icon(),
                titleColor = it.textColor(),
                iconColor = it.iconColor()
            )
        }
        .toPersistentList()

    Column(modifier = modifier.fillMaxWidth()) {
        Menu(
            items = profileMenu,
            onClick = onProfileMenuClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = SettingsMenu.title().text(),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Menu(
            items = settingsMenu,
            onClick = onSettingsMenuClick
        )
    }
}

@AppPreview
@Composable
private fun ProfileMenuContentPreview() {
    PreviewContainer {
        ProfileMenuContent(role = RoleEnumState.ADMIN, onProfileMenuClick = {}, onSettingsMenuClick = {})
    }
}
