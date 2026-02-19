package com.grippo.dialog.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.RoleEnumState
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.components.user.UserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Logout
import com.grippo.design.resources.provider.logout_btn
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    loaders: ImmutableSet<ProfileLoader>,
    contract: ProfileContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    if (state.user != null) {
        UserCard(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            value = state.user
        )
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val profileMenu = ProfileMenu.entries.map {
        it to MenuItem(UiText.Str(it.text()), it.icon())
    }.toPersistentList()

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .padding(start = AppTokens.dp.contentPadding.subContent),
        text = ProfileMenu.title(),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

    Menu(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        items = profileMenu,
        onClick = contract::onProfileMenuClick
    )

    if (state.user?.role == RoleEnumState.ADMIN) {
        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        val settingsMenu = SettingsMenu.entries.map {
            it to MenuItem(UiText.Str(it.text()), it.icon())
        }.toPersistentList()

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .padding(start = AppTokens.dp.contentPadding.subContent),
            text = SettingsMenu.title(),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Menu(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            items = settingsMenu,
            onClick = contract::onSettingsMenuClick
        )
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        style = ButtonStyle.Error,
        content = ButtonContent.Text(
            startIcon = ButtonIcon.Icon(AppTokens.icons.Logout),
            text = AppTokens.strings.res(Res.string.logout_btn),
        ),
        onClick = contract::onLogoutClick
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ProfileScreen(
            state = ProfileState(
                user = stubUser()
            ),
            contract = ProfileContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
