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
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.user.DeleteUserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.analytics_consent_description
import com.grippo.design.resources.provider.analytics_consent_title
import com.grippo.design.resources.provider.icons.Logout
import com.grippo.design.resources.provider.logout_btn
import com.grippo.design.resources.provider.settings
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileSettingsScreen(
    state: ProfileSettingsState,
    loaders: ImmutableSet<ProfileSettingsLoader>,
    contract: ProfileSettingsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.settings),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack)
    )

    val consentTitle = AppTokens.strings.res(Res.string.analytics_consent_title)

    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Row(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = consentTitle,
                    style = AppTokens.typography.h6(),
                    color = AppTokens.colors.text.primary,
                )
                Text(
                    text = AppTokens.strings.res(Res.string.analytics_consent_description),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            Toggle(
                checked = state.analyticsConsent == true,
                enabled = state.analyticsConsent != null && ProfileSettingsLoader.AnalyticsConsent !in loaders,
                contentDescription = consentTitle,
                onCheckedChange = { contract.onAnalyticsConsentChange(state.analyticsConsent != true) },
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            style = ButtonStyle.Error,
            state = if (loaders.contains(ProfileSettingsLoader.LogoutButton)) {
                ButtonState.Loading
            } else {
                ButtonState.Enabled
            },
            content = ButtonContent.Text(
                startIcon = ButtonIcon.Icon(AppTokens.icons.Logout),
                text = AppTokens.strings.res(Res.string.logout_btn),
            ),
            onClick = contract::onLogoutClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        DeleteUserCard(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            isDeleteLoading = loaders.contains(ProfileSettingsLoader.DeleteAccountButton),
            onDeleteClick = contract::onDeleteAccount
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@AppPreview
@Composable
private fun ProfileSettingsPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = ProfileSettingsState(analyticsConsent = false),
            loaders = persistentSetOf(),
            contract = ProfileSettingsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun AnalyticsConsentEnabledPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = ProfileSettingsState(analyticsConsent = true),
            loaders = persistentSetOf(),
            contract = ProfileSettingsContract.Empty,
        )
    }
}
