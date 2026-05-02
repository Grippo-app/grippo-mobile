package com.grippo.core.state.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.details
import com.grippo.design.resources.provider.edit_btn
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.icons.Edit
import com.grippo.design.resources.provider.icons.Info
import com.grippo.design.resources.provider.selected
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public sealed class TrainingMenu : PickerMenuItem {

    public data object Details : TrainingMenu() {
        override val id: String get() = "details"

        override fun text(): UiText = UiText.Res(Res.string.details)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Info

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.brand.color5
    }

    public data object Edit : TrainingMenu() {
        override val id: String get() = "edit"

        override fun text(): UiText = UiText.Res(Res.string.edit_btn)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Edit

        @Composable
        override fun textColor(): Color = AppTokens.colors.text.primary

        @Composable
        override fun iconColor(): Color = AppTokens.colors.brand.color2
    }

    public data object Delete : TrainingMenu() {
        override val id: String get() = "delete"

        override fun text(): UiText = UiText.Res(Res.string.delete_btn)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Cancel

        @Composable
        override fun textColor(): Color = AppTokens.colors.semantic.error

        @Composable
        override fun iconColor(): Color = AppTokens.colors.semantic.error
    }

    public companion object {
        public val entries: ImmutableList<TrainingMenu> = persistentListOf(Details, Edit, Delete)

        public fun title(): UiText = UiText.Res(Res.string.selected)
    }
}
