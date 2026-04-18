package com.grippo.core.state.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed interface PickerMenuItem {
    public val id: String

    public fun text(): UiText

    @Composable
    public fun icon(): ImageVector

    @Composable
    public fun color(): Color = Color.Unspecified
}
