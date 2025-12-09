package com.grippo.design.components.tab

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowDown

@Immutable
public data class TabItem(
    val text: UiText,
    val icon: ImageVector
)

@Composable
public fun <KEY> Tab(
    modifier: Modifier = Modifier,
    item: Pair<KEY, TabItem>,
    isSelected: Boolean,
    onSelect: (KEY) -> Unit,
) {
    val clickProvider = remember(item.first) { { onSelect.invoke(item.first) } }

    Icon(
        modifier = modifier
            .scalableClick(onClick = clickProvider)
            .padding(
                horizontal = AppTokens.dp.tab.horizontalPadding,
                vertical = AppTokens.dp.tab.verticalPadding,
            )
            .size(AppTokens.dp.tab.icon),
        imageVector = item.second.icon,
        tint = if (isSelected) {
            AppTokens.colors.segment.active
        } else {
            AppTokens.colors.segment.inactive
        },
        contentDescription = null
    )
}

@AppPreview
@Composable
private fun SegmentPreview() {
    PreviewContainer {
        Tab(
            item = "Box" to TabItem(
                text = UiText.Str("Box"),
                icon = AppTokens.icons.ArrowDown
            ),
            isSelected = true,
            onSelect = {}
        )
    }
}