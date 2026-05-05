package com.grippo.dialog.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.core.state.profile.RoleEnumState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.components.user.UserCard
import com.grippo.design.components.user.UserCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
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
            value = state.user,
            style = UserCardStyle.Compact
        )
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

    val basePadding = PaddingValues(
        horizontal = AppTokens.dp.dialog.horizontalPadding,
    )

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.dialog,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier
                    .fillMaxWidth()
                    .weight(1f, false),
                contentPadding = resolvedPadding
            ) {
                item {
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
                        .filter { !(state.user?.role != RoleEnumState.ADMIN && it == SettingsMenu.Debug) }
                        .map {
                            it to MenuItem(
                                title = it.text(),
                                icon = it.icon(),
                                titleColor = it.textColor(),
                                iconColor = it.iconColor()
                            )
                        }
                        .toPersistentList()

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Menu(
                            items = profileMenu,
                            onClick = contract::onProfileMenuClick
                        )

                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = SettingsMenu.title().text(),
                            style = AppTokens.typography.h4(),
                            color = AppTokens.colors.text.tertiary
                        )

                        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

                        Menu(
                            items = settingsMenu,
                            onClick = contract::onSettingsMenuClick
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
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
