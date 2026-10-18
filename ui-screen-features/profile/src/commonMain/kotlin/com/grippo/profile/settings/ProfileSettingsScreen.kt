package com.grippo.profile.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.menu.MenuActionRow
import com.grippo.design.components.menu.MenuSection
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.user.UserProfileHeader
import com.grippo.design.components.user.UserProfileHeaderStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.analytics_consent_description
import com.grippo.design.resources.provider.analytics_consent_title
import com.grippo.design.resources.provider.delete_account
import com.grippo.design.resources.provider.icons.Lock
import com.grippo.design.resources.provider.icons.Logout
import com.grippo.design.resources.provider.icons.Trash
import com.grippo.design.resources.provider.logout_btn
import com.grippo.design.resources.provider.profile_account
import com.grippo.design.resources.provider.profile_settings_delete_description
import com.grippo.design.resources.provider.profile_settings_logout_description
import com.grippo.design.resources.provider.profile_settings_privacy_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileSettingsScreen(
    state: ProfileSettingsState,
    loaders: ImmutableSet<ProfileSettingsLoader>,
    contract: ProfileSettingsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val logoutLoading = ProfileSettingsLoader.LogoutButton in loaders
    val deleteLoading = ProfileSettingsLoader.DeleteAccountButton in loaders
    val consentTitle = AppTokens.strings.res(Res.string.analytics_consent_title)

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.profile_account),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
    ) {
        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        UserProfileHeader(
            value = state.user,
            style = UserProfileHeaderStyle.Account,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        MenuSection(
            title = AppTokens.strings.res(Res.string.profile_settings_privacy_title),
            icon = AppTokens.icons.Lock,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = consentTitle,
                        style = AppTokens.typography.h6(),
                        color = AppTokens.colors.text.primary,
                    )
                    Toggle(
                        checked = state.analyticsConsent == true,
                        enabled = state.analyticsConsent != null && ProfileSettingsLoader.AnalyticsConsent !in loaders,
                        contentDescription = consentTitle,
                        onCheckedChange = { contract.onAnalyticsConsentChange(state.analyticsConsent != true) },
                    )
                }
                Text(
                    text = AppTokens.strings.res(Res.string.analytics_consent_description),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        MenuActionRow(
            title = AppTokens.strings.res(Res.string.logout_btn),
            description = AppTokens.strings.res(Res.string.profile_settings_logout_description),
            icon = AppTokens.icons.Logout,
            state = when {
                logoutLoading -> ButtonState.Loading
                deleteLoading -> ButtonState.Disabled
                else -> ButtonState.Enabled
            },
            onClick = contract::onLogoutClick,
        )

        ContentDivider(
            modifier = Modifier.padding(
                start = AppTokens.dp.menu.item.iconContainer + AppTokens.dp.menu.item.space,
            ),
        )

        MenuActionRow(
            title = AppTokens.strings.res(Res.string.delete_account),
            description = AppTokens.strings.res(Res.string.profile_settings_delete_description),
            icon = AppTokens.icons.Trash,
            state = when {
                deleteLoading -> ButtonState.Loading
                logoutLoading -> ButtonState.Disabled
                else -> ButtonState.Enabled
            },
            onClick = contract::onDeleteAccount,
            titleColor = AppTokens.colors.semantic.error,
            iconColor = AppTokens.colors.semantic.error,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

private fun profileSettingsPreviewState(): ProfileSettingsState = ProfileSettingsState(
    analyticsConsent = false,
    user = stubUser().copy(
        id = "profile-settings-preview",
        name = "Alex",
        email = "alex@grippo.app",
    ),
)

@AppPreview
@Composable
private fun ProfileSettingsPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = profileSettingsPreviewState(),
            loaders = persistentSetOf(),
            contract = ProfileSettingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun AnalyticsConsentEnabledPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = profileSettingsPreviewState().copy(analyticsConsent = true),
            loaders = persistentSetOf(),
            contract = ProfileSettingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileSettingsConsentUnavailablePreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = profileSettingsPreviewState().copy(analyticsConsent = null),
            loaders = persistentSetOf(),
            contract = ProfileSettingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileSettingsConsentLoadingPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = profileSettingsPreviewState().copy(analyticsConsent = true),
            loaders = persistentSetOf(ProfileSettingsLoader.AnalyticsConsent),
            contract = ProfileSettingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileSettingsLogoutLoadingPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = profileSettingsPreviewState(),
            loaders = persistentSetOf(ProfileSettingsLoader.LogoutButton),
            contract = ProfileSettingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileSettingsDeleteLoadingPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = profileSettingsPreviewState(),
            loaders = persistentSetOf(ProfileSettingsLoader.DeleteAccountButton),
            contract = ProfileSettingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileSettingsIdentityUnavailablePreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = profileSettingsPreviewState().copy(user = null),
            loaders = persistentSetOf(),
            contract = ProfileSettingsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileSettingsLongIdentityPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = profileSettingsPreviewState().copy(
                user = stubUser().copy(
                    id = "profile-settings-preview",
                    name = "Олександр Костянтинович Петренко",
                    email = "oleksandr.kostiantynovych.petrenko@athlete.grippo.app",
                ),
            ),
            loaders = persistentSetOf(),
            contract = ProfileSettingsContract.Empty,
        )
    }
}
