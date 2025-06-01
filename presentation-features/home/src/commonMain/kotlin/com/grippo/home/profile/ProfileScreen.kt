package com.grippo.home.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.grippo.presentation.api.user.models.stubUser
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    loaders: ImmutableSet<ProfileLoader>,
    contract: ProfileContract
) = BaseComposeScreen {
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
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (state.user != null) {
                UserCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.user
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val menu = ProfileMenu.entries.map {
                it to MenuItem(it.text(), it.icon())
            }.toPersistentList()

            Menu(
                items = menu,
                onClick = contract::onMenuClick
            )

            Spacer(modifier = Modifier.height(24.dp))

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
private fun ProfileScreenEmpty() {
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