package com.grippo.design.components.user.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun UserExperienceBadge(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    color: Color,
) {
    val contentColor = AppTokens.colors.static.white

    Row(
        modifier = modifier
            .background(
                color = color,
                shape = RoundedCornerShape(AppTokens.dp.userCard.experience.radius)
            )
            .padding(
                horizontal = AppTokens.dp.userCard.experience.horizontalPadding,
                vertical = AppTokens.dp.userCard.experience.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.experience.space)
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.userCard.experience.icon),
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )

        Text(
            text = text,
            style = AppTokens.typography.b13Semi(),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreview
@Composable
private fun UserExperienceBadgePreview() {
    PreviewContainer {
        ExperienceEnumState.entries.forEach { experience ->
            UserExperienceBadge(
                text = experience.title().text(),
                icon = experience.icon(),
                color = experience.color(),
            )
        }
    }
}
