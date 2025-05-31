package com.grippo.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.menu.MenuCard
import com.grippo.design.components.user.UserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Book
import com.grippo.design.resources.icons.Edit
import com.grippo.design.resources.icons.LogOut
import com.grippo.design.resources.logout_btn
import com.grippo.presentation.api.user.models.stubUser
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    loaders: ImmutableSet<ProfileLoader>,
    contract: ProfileContract
) = BaseComposeScreen {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppTokens.dp.paddings.screenHorizontal,
                vertical = AppTokens.dp.paddings.screenVertical
            ).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (state.user != null) {
            UserCard(
                modifier = Modifier.fillMaxWidth(),
                value = state.user
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val shape = RoundedCornerShape(AppTokens.dp.shape.large)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = shape)
                .background(AppTokens.colors.background.secondary)
                .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
        ) {

            MenuCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Edit profile",
                icon = AppTokens.icons.Edit,
                onClick = {}
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.paddings.mediumHorizontal)
                    .fillMaxWidth(),
                color = AppTokens.colors.divider.default
            )

            MenuCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Exercise library",
                icon = AppTokens.icons.Book,
                onClick = {}
            )
        }

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