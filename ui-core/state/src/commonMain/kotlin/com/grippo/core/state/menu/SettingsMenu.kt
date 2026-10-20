package com.grippo.core.state.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.community
import com.grippo.design.resources.provider.debug
import com.grippo.design.resources.provider.settings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public sealed class SettingsMenu {

    public abstract fun text(): UiText

    @Composable
    public abstract fun textColor(): Color

    public data object Settings : SettingsMenu() {
        override fun text(): UiText = UiText.Res(Res.string.settings)

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary
    }

    public data object Social : SettingsMenu() {
        override fun text(): UiText = UiText.Res(Res.string.community)

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary
    }

    public data object Debug : SettingsMenu() {
        override fun text(): UiText = UiText.Res(Res.string.debug)

        @Composable
        override fun textColor(): Color = AppTokens.colors.semantic.error
    }

    public companion object {
        public val entries: ImmutableList<SettingsMenu> = persistentListOf(Settings, Social, Debug)
    }
}
