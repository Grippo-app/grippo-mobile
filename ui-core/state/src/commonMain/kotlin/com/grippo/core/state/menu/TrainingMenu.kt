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
import com.grippo.design.resources.provider.icons.EyeOn
import com.grippo.design.resources.provider.selected

@Immutable
public sealed class TrainingMenu : PickerMenuItem {

    public data object Details : TrainingMenu() {
        override val id: String get() = "details"
        override fun text(): UiText = UiText.Res(Res.string.details)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.EyeOn
    }

    public data object Edit : TrainingMenu() {
        override val id: String get() = "edit"
        override fun text(): UiText = UiText.Res(Res.string.edit_btn)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Edit
    }

    public data object Delete : TrainingMenu() {
        override val id: String get() = "delete"
        override fun text(): UiText = UiText.Res(Res.string.delete_btn)

        @Composable
        override fun icon(): ImageVector = AppTokens.icons.Cancel

        @Composable
        override fun color(): Color = AppTokens.colors.semantic.error
    }

    public companion object {
        public val entries: List<TrainingMenu> = listOf(Details, Edit, Delete)

        public fun title(): UiText = UiText.Res(Res.string.selected)
    }
}
