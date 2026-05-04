package com.grippo.design.components.user.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Height
import com.grippo.design.resources.provider.icons.Weight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class UserStatEntry(
    val icon: ImageVector?,
    val text: String,
)

@Composable
internal fun UserStatsStrip(
    modifier: Modifier = Modifier,
    stats: ImmutableList<UserStatEntry>,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.userCard.stats.radius)
            )
            .height(intrinsicSize = IntrinsicSize.Min)
            .padding(vertical = AppTokens.dp.userCard.stats.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEachIndexed { index, stat ->
            UserStatItem(
                modifier = Modifier.weight(1f),
                icon = stat.icon,
                text = stat.text,
            )

            if (index < stats.lastIndex) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = AppTokens.dp.userCard.stats.dividerWidth,
                    color = AppTokens.colors.divider.default
                )
            }
        }
    }
}

@Composable
private fun UserStatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    text: String,
) {
    Row(
        modifier = modifier.padding(horizontal = AppTokens.dp.userCard.stats.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.userCard.stats.icon),
                imageVector = icon,
                contentDescription = null,
                tint = AppTokens.colors.icon.secondary,
            )

            Spacer(Modifier.width(AppTokens.dp.userCard.stats.space))
        }

        Text(
            text = text,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun UserStatsStripFullPreview() {
    PreviewContainer {
        UserStatsStrip(
            stats = persistentListOf(
                UserStatEntry(icon = AppTokens.icons.Height, text = "179 см"),
                UserStatEntry(icon = AppTokens.icons.Weight, text = "80.0 кг"),
                UserStatEntry(icon = null, text = "груд. 01, 2025"),
            )
        )
    }
}

@AppPreview
@Composable
private fun UserStatsStripPartialPreview() {
    PreviewContainer {
        UserStatsStrip(
            stats = persistentListOf(
                UserStatEntry(icon = AppTokens.icons.Height, text = "179 см"),
                UserStatEntry(icon = AppTokens.icons.Weight, text = "80.0 кг"),
            )
        )

        UserStatsStrip(
            stats = persistentListOf(
                UserStatEntry(icon = null, text = "груд. 01, 2025"),
            )
        )
    }
}
