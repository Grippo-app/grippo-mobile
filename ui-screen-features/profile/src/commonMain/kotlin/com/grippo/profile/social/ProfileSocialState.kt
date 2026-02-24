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
        urls = listOf(
            "https://www.youtube.com/@GrippoApp",
            "https://www.youtube.com/@GrippoApp/videos",
            "https://www.youtube.com/results?search_query=GrippoApp"
        )
    )
}
