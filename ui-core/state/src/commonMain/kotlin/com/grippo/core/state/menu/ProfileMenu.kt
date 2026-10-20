package com.grippo.core.state.menu

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_menu
import com.grippo.design.resources.provider.profile_menu_excluded_muscles
import com.grippo.design.resources.provider.profile_menu_experience
import com.grippo.design.resources.provider.profile_menu_missing_equipment
import com.grippo.design.resources.provider.weight_and_height

@Immutable
public sealed class ProfileMenu {

    public abstract fun text(): UiText

    public data object Body : ProfileMenu() {
        override fun text(): UiText = UiText.Res(Res.string.weight_and_height)
    }

    public data object Experience : ProfileMenu() {
        override fun text(): UiText = UiText.Res(Res.string.profile_menu_experience)
    }

    public data object Muscles : ProfileMenu() {
        override fun text(): UiText = UiText.Res(Res.string.profile_menu_excluded_muscles)
    }

    public data object Equipment : ProfileMenu() {
        override fun text(): UiText = UiText.Res(Res.string.profile_menu_missing_equipment)
    }

    public data object Goal : ProfileMenu() {
        override fun text(): UiText = UiText.Res(Res.string.goal_menu)
    }
}
