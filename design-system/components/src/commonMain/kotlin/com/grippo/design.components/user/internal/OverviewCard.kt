package com.grippo.design.components.user.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.User

@Composable
internal fun OverviewCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = modifier
            .padding(
                horizontal = AppTokens.dp.overviewCard.horizontalPadding,
                vertical = AppTokens.dp.overviewCard.verticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.overviewCard.icon),
            imageVector = icon,
            tint = AppTokens.colors.icon.accent,
            contentDescription = null
        )

        Text(
            text = title,
            style = AppTokens.typography.b13Bold(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@AppPreview
@Composable
private fun OverviewItemPreview() {
    PreviewContainer {
        OverviewCard(
            title = "Hello World",
            icon = AppTokens.icons.User
        )
    }
}