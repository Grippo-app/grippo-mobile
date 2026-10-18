package com.grippo.profile.social

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.menu.SocialMenu
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.community
import com.grippo.design.resources.provider.social_hero_subtitle
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ProfileSocialScreen(
    state: ProfileSocialState,
    loaders: ImmutableSet<ProfileSocialLoader>,
    contract: ProfileSocialContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.community),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.social_hero_subtitle),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Start,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val items = SocialMenu.entries
        .map { channel ->
            channel to MenuItem(
                title = channel.text(),
                icon = channel.icon(),
                titleColor = channel.textColor(),
                iconColor = channel.iconColor()
            )
        }.toPersistentList()

    Menu(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        items = items,
        onClick = contract::onOpenLink
    )

    Spacer(modifier = Modifier.weight(1f))

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ProfileSocialPreview() {
    PreviewContainer {
        ProfileSocialScreen(
            state = ProfileSocialState,
            loaders = persistentSetOf(),
            contract = ProfileSocialContract.Empty,
        )
    }
}
