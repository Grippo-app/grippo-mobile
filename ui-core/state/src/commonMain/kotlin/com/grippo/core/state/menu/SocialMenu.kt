package com.grippo.core.state.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.follow_us
import com.grippo.design.resources.provider.icons.Instagram
import com.grippo.design.resources.provider.icons.Threads
import com.grippo.design.resources.provider.icons.Tiktok
import com.grippo.design.resources.provider.icons.Youtube
import com.grippo.design.resources.provider.instagram
import com.grippo.design.resources.provider.threads
import com.grippo.design.resources.provider.tiktok
import com.grippo.design.resources.provider.youtube
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public sealed class SocialMenu : PickerMenuItem {

    public abstract val urls: ImmutableList<String>

    public data object Instagram : SocialMenu() {
        override val id: String get() = "instagram"

        override val urls: ImmutableList<String> =
            persistentListOf("https://www.instagram.com/grippo.app/")

        override fun text(): UiText = UiText.Res(Res.string.instagram)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Instagram

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.icon.primary
    }

    public data object TikTok : SocialMenu() {
        override val id: String get() = "tiktok"

        override val urls: ImmutableList<String> =
            persistentListOf("https://www.tiktok.com/@grippoapp")

        override fun text(): UiText = UiText.Res(Res.string.tiktok)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Tiktok

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.icon.primary
    }

    public data object YouTube : SocialMenu() {
        override val id: String get() = "youtube"

        override val urls: ImmutableList<String> =
            persistentListOf("https://www.youtube.com/@GrippoApp")

        override fun text(): UiText = UiText.Res(Res.string.youtube)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Youtube

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.icon.primary
    }

    public data object Threads : SocialMenu() {
        override val id: String get() = "threads"

        override val urls: ImmutableList<String> =
            persistentListOf("https://www.threads.com/@grippo.app")

        override fun text(): UiText = UiText.Res(Res.string.threads)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Threads

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.icon.primary
    }

    public companion object {
        public val entries: ImmutableList<SocialMenu> =
            persistentListOf(Instagram, TikTok, YouTube, Threads)

        public fun title(): UiText = UiText.Res(Res.string.follow_us)
    }
}
