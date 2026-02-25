package com.grippo.profile.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Instagram
import com.grippo.design.resources.provider.icons.Threads
import com.grippo.design.resources.provider.icons.Tiktok
import com.grippo.design.resources.provider.icons.Youtube
import com.grippo.design.resources.provider.instagram
import com.grippo.design.resources.provider.threads
import com.grippo.design.resources.provider.tiktok
import com.grippo.design.resources.provider.youtube

@Immutable
internal data object ProfileSocialState

@Immutable
internal enum class SocialChannel(
    val title: UiText,
    val urls: List<String>,
) {
    Instagram(
        title = UiText.Res(Res.string.instagram),
        urls = listOf("https://www.instagram.com/grippo.app/")
    ),
    TikTok(
        title = UiText.Res(Res.string.tiktok),
        urls = listOf("https://www.tiktok.com/@grippoapp")
    ),
    YouTube(
        title = UiText.Res(Res.string.youtube),
        urls = listOf("https://www.youtube.com/@GrippoApp")
    ),
    Threads(
        title = UiText.Res(Res.string.threads),
        urls = listOf("https://www.threads.com/@grippo.app")
    );

    @Composable
    fun icon(): ImageVector {
        return when (this) {
            Instagram -> AppTokens.icons.Instagram
            TikTok -> AppTokens.icons.Tiktok
            YouTube -> AppTokens.icons.Youtube
            Threads -> AppTokens.icons.Threads
        }
    }
}
