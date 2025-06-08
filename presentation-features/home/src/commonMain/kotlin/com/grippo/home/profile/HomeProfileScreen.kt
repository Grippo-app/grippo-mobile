package com.grippo.home.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.user.UserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.LogOut
import com.grippo.design.resources.logout_btn
import com.grippo.design.resources.profile
import com.grippo.presentation.api.profile.models.stubUser
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun HomeProfileScreen(
    state: HomeProfileState,
    loaders: ImmutableSet<HomeProfileLoader>,
    contract: HomeProfileContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {
    Column {
        Toolbar(
            modifier = Modifier.fillMaxWidth(),
            title = AppTokens.strings.res(Res.string.profile),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppTokens.dp.screen.horizontalPadding,
                    vertical = AppTokens.dp.contentPadding.content
                ).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {

            if (state.user != null) {
                UserCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.user
                )
            }

            val menu = HomeProfileMenu.entries.map {
                it to MenuItem(it.text(), it.icon())
            }.toPersistentList()

            Menu(
                items = menu,
                onClick = contract::onMenuClick
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Secondary,
                startIcon = AppTokens.icons.LogOut,
                text = AppTokens.strings.res(Res.string.logout_btn),
                onClick = contract::onLogoutClick
            )
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        HomeProfileScreen(
            state = HomeProfileState(
                user = stubUser()
            ),
            contract = HomeProfileContract.Empty,
            loaders = persistentSetOf()
        )
    }
}