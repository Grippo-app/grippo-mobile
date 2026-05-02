package com.grippo.core.state.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_menu
import com.grippo.design.resources.provider.icons.Body
import com.grippo.design.resources.provider.icons.Kettlebell
import com.grippo.design.resources.provider.icons.Medal
import com.grippo.design.resources.provider.icons.Muscle
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.profile
import com.grippo.design.resources.provider.profile_menu_excluded_muscles
import com.grippo.design.resources.provider.profile_menu_experience
import com.grippo.design.resources.provider.profile_menu_missing_equipment
import com.grippo.design.resources.provider.weight_and_height
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public sealed class ProfileMenu : PickerMenuItem {

    public data object Body : ProfileMenu() {
        override val id: String get() = "body"

        override fun text(): UiText = UiText.Res(Res.string.weight_and_height)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Body

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.brand.color5
    }

    public data object Experience : ProfileMenu() {
        override val id: String get() = "experience"

        override fun text(): UiText = UiText.Res(Res.string.profile_menu_experience)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Medal

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.brand.color4
    }

    public data object Muscles : ProfileMenu() {
        override val id: String get() = "muscles"

        override fun text(): UiText = UiText.Res(Res.string.profile_menu_excluded_muscles)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Muscle

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.brand.color2
    }

    public data object Equipment : ProfileMenu() {
        override val id: String get() = "equipment"
        override fun text(): UiText = UiText.Res(Res.string.profile_menu_missing_equipment)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Kettlebell

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.brand.color3
    }

    public data object Goal : ProfileMenu() {
        override val id: String get() = "goal"

        override fun text(): UiText = UiText.Res(Res.string.goal_menu)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Trophy

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.brand.color1
    }

    public companion object {
        public val entries: ImmutableList<ProfileMenu> =
            persistentListOf(Body, Experience, Muscles, Equipment, Goal)

        public fun title(): UiText = UiText.Res(Res.string.profile)
    }
}
