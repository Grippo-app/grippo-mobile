package com.grippo.home.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.user.UserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.logout_btn
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    loaders: ImmutableSet<ProfileLoader>,
    contract: ProfileContract
) = BaseComposeScreen {
    Column(
        modifier = Modifier
            .fillMaxSize()
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

        Spacer(Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Secondary,
            text = AppTokens.strings.res(Res.string.logout_btn),
            onClick = contract::onLogoutClick
        )
    }
}