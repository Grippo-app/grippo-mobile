package com.grippo.design.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowRight

@Composable
public fun MenuValueRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    loading: Boolean = false,
    showDivider: Boolean = true,
    titleColor: Color = AppTokens.colors.text.primary,
    icon: ImageVector? = null,
    iconColor: Color = AppTokens.colors.icon.primary,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val valueMaxWidth = maxWidth * 0.45f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppTokens.dp.button.medium.height)
                    .scalableClick(role = Role.Button, onClick = onClick)
                    .padding(vertical = AppTokens.dp.contentPadding.subContent),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.let {
                    Icon(
                        modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                        imageVector = it,
                        tint = iconColor,
                        contentDescription = null,
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = AppTokens.typography.b14Semi(),
                    color = titleColor,
                )
                MenuRowValue(
                    modifier = Modifier.widthIn(max = valueMaxWidth),
                    value = value,
                    loading = loading,
                    textAlign = TextAlign.End,
                )

                Icon(
                    modifier = Modifier.size(AppTokens.dp.menu.item.icon),
                    imageVector = AppTokens.icons.ArrowRight,
                    tint = AppTokens.colors.icon.secondary,
                    contentDescription = null,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = if (icon == null) Modifier else Modifier.padding(
                    start = AppTokens.dp.menu.item.icon + AppTokens.dp.contentPadding.subContent,
                ),
                color = AppTokens.colors.divider.default,
            )
        }
    }
}

@Composable
private fun MenuRowValue(
    value: String?,
    loading: Boolean,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    when {
        loading -> Box(
            modifier = modifier
                .width(AppTokens.dp.userCard.avatar.small)
                .height(AppTokens.dp.menu.item.icon)
                .clip(RoundedCornerShape(AppTokens.dp.contentPadding.text))
                .background(AppTokens.colors.divider.default)
                .shimmer()
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
        )
        value != null -> Text(
            modifier = modifier,
            text = value,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = textAlign,
        )
    }
}

@AppPreview
@Composable
private fun MenuValueRowPreview() {
    PreviewContainer {
        MenuSection(title = "Body") {
            MenuValueRow(title = "Weight", value = "82 kg", onClick = {})
            MenuValueRow(title = "Height", loading = true, onClick = {}, showDivider = false)
        }
    }
}
