package com.grippo.core.state.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public sealed class SettingsMenu : PickerMenuItem {

    public data object Settings : SettingsMenu() {
        override val id: String get() = "settings"

        override fun text(): UiText = UiText.Res(Res.string.settings)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Filter

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.icon.secondary
    }

    public data object Social : SettingsMenu() {
        override val id: String get() = "social"

        override fun text(): UiText = UiText.Res(Res.string.follow_us)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.ChatAlt

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.brand.color5
    }

    public data object Debug : SettingsMenu() {
        override val id: String get() = "debug"

        override fun text(): UiText = UiText.Res(Res.string.debug)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Bug

        @Composable
        override fun textColor(): Color = AppTokens.colors.semantic.error

        @Composable
        override fun iconColor(): Color = AppTokens.colors.semantic.error
    }

    public companion object {
        public val entries: ImmutableList<SettingsMenu> = persistentListOf(Settings, Social, Debug)

        public fun title(): UiText = UiText.Res(Res.string.more)
    }
}
