package com.grippo.profile.menu

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.user.UserCard
import com.grippo.design.components.user.UserCardStyle
import com.grippo.design.core.AppTheme
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.profile
import com.grippo.profile.menu.components.ProfileMenuContent
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileMenuScreen(
    state: ProfileMenuState,
    loaders: ImmutableSet<ProfileMenuLoader>,
    contract: ProfileMenuContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.profile),
        style = ToolbarStyle.Transparent,
    )
    if (state.user == null && ProfileMenuLoader.User in loaders) {
        Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        return@BaseComposeScreen
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f).navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = AppTokens.dp.screen.horizontalPadding,
            end = AppTokens.dp.screen.horizontalPadding,
            top = AppTokens.dp.contentPadding.content,
            bottom = AppTokens.dp.screen.verticalPadding,
        ),
    ) {
        state.user?.let { user ->
            item(key = "user") {
                UserCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = user,
                    style = UserCardStyle.Compact,
                )
                Spacer(Modifier.size(AppTokens.dp.contentPadding.content))
            }
        }
        item(key = "menu") {
            ProfileMenuContent(
                role = state.user?.role,
                onProfileMenuClick = contract::onProfileMenuClick,
                onSettingsMenuClick = contract::onSettingsMenuClick,
            )
        }
    }
}

@AppPreview
@Composable
private fun ProfileMenuScreenPreview() {
    PreviewContainer {
        ProfileMenuScreen(
            state = ProfileMenuState(user = stubUser()),
            loaders = persistentSetOf(),
            contract = ProfileMenuContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileMenuScreenLoadingPreview() {
    PreviewContainer {
        ProfileMenuScreen(
            state = ProfileMenuState(),
            loaders = persistentSetOf(ProfileMenuLoader.User),
            contract = ProfileMenuContract.Empty,
        )
    }
}

@Preview(
    name = "Profile • UK • Long text • 200%",
    widthDp = 360,
    heightDp = 800,
    fontScale = 2f,
    locale = "uk",
)
@Composable
private fun ProfileMenuScreenLargeFontPreview() {
    PreviewContainer {
        AppTheme(darkTheme = isSystemInDarkTheme(), localeTag = "uk") {
            ProfileMenuScreen(
                state = ProfileMenuState(
                    user = stubUser().copy(
                        name = "Олександр Костянтинович",
                        email = "oleksandr.kostiantynovych@example.com",
                    )
                ),
                loaders = persistentSetOf(),
                contract = ProfileMenuContract.Empty,
            )
        }
    }
}
