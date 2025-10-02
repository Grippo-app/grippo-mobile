package com.grippo.home.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.user.UserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.LogOut
import com.grippo.design.resources.provider.icons.Rocket
import com.grippo.design.resources.provider.logout_btn
import com.grippo.design.resources.provider.profile
import com.grippo.design.resources.provider.start_workout
import com.grippo.state.formatters.UiText
import com.grippo.state.profile.stubUser
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun HomeProfileScreen(
    state: HomeProfileState,
    loaders: ImmutableSet<HomeProfileLoader>,
    contract: HomeProfileContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.profile),
    )

    val activity = HomeProfileActivityMenu.entries.map {
        it to MenuItem(UiText.Str(it.text()), it.icon())
    }.toPersistentList()

    val settings = HomeProfileSettingsMenu.entries.map {
        it to MenuItem(UiText.Str(it.text()), it.icon())
    }.toPersistentList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.contentPadding.content
            ).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (state.user != null) {
            UserCard(
                modifier = Modifier.fillMaxWidth(),
                value = state.user
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Primary,
            content = ButtonContent.Text(
                startIcon = AppTokens.icons.Rocket,
                text = AppTokens.strings.res(Res.string.start_workout),
            ),
            onClick = contract::onStartTrainingClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Menu(
            items = activity,
            onClick = contract::onActivityMenuClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Menu(
            items = settings,
            onClick = contract::onSettingsMenuClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Error,
            content = ButtonContent.Text(
                startIcon = AppTokens.icons.LogOut,
                text = AppTokens.strings.res(Res.string.logout_btn),
            ),
            onClick = contract::onLogoutClick
        )
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