package com.grippo.home.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.user.UserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.LogOut
import com.grippo.design.resources.icons.Rocket
import com.grippo.design.resources.logout_btn
import com.grippo.design.resources.profile
import com.grippo.design.resources.start_workout
import com.grippo.presentation.api.profile.models.stubUser
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun HomeProfileScreen(
    state: HomeProfileState,
    loaders: ImmutableSet<HomeProfileLoader>,
    contract: HomeProfileContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {

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
            startIcon = AppTokens.icons.Rocket,
            text = AppTokens.strings.res(Res.string.start_workout),
            onClick = contract::onStartWorkoutClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = HomeProfileActivityMenu.title(),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Menu(
            items = activity,
            onClick = contract::onActivityMenuClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = HomeProfileSettingsMenu.title(),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Menu(
            items = settings,
            onClick = contract::onSettingsMenuClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Button(
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Secondary,
            startIcon = AppTokens.icons.LogOut,
            text = AppTokens.strings.res(Res.string.logout_btn),
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