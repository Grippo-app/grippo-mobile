package com.grippo.core.state.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.debug
import com.grippo.design.resources.provider.follow_us
import com.grippo.design.resources.provider.icons.Bug
import com.grippo.design.resources.provider.icons.ChatAlt
import com.grippo.design.resources.provider.icons.Filter
import com.grippo.design.resources.provider.more
import com.grippo.design.resources.provider.settings

@Immutable
public sealed class SettingsMenu : PickerMenuItem {

    public data object Settings : SettingsMenu() {
        override val id: String get() = "settings"
        override fun text(): UiText = UiText.Res(Res.string.settings)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Filter
    }

    public data object Social : SettingsMenu() {
        override val id: String get() = "social"
        override fun text(): UiText = UiText.Res(Res.string.follow_us)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.ChatAlt
    }

    public data object Debug : SettingsMenu() {
        override val id: String get() = "debug"
        override fun text(): UiText = UiText.Res(Res.string.debug)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Bug
    }

    public companion object {
        public val entries: List<SettingsMenu> = listOf(Settings, Social, Debug)

        public fun title(): UiText = UiText.Res(Res.string.more)
    }
}
