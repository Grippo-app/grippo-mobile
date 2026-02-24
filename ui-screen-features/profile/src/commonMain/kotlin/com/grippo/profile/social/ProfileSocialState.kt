package com.grippo.profile.social

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.instagram
import com.grippo.design.resources.provider.tiktok
import com.grippo.design.resources.provider.youtube

@Immutable
internal data object ProfileSocialState

@Immutable
internal enum class SocialChannel(
    val title: UiText,
    val url: String,
) {
    Instagram(
        title = UiText.Res(Res.string.instagram),
        url = "https://www.instagram.com/grippo.app/"
    ),
    TikTok(
        title = UiText.Res(Res.string.tiktok),
        url = "https://www.tiktok.com/@grippoapp"
    ),
    YouTube(
        title = UiText.Res(Res.string.youtube),
        url = "https://www.youtube.com/@GrippoApp"
    )
}