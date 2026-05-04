package com.grippo.design.components.user.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun UserCardCompact(
    modifier: Modifier = Modifier,
    value: UserState,
) {
    val experienceColor = value.experience.color()

    Row(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.userCard.compact.radius)
            )
            .padding(
                horizontal = AppTokens.dp.userCard.compact.horizontalPadding,
                vertical = AppTokens.dp.userCard.compact.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.userCard.compact.space)
    ) {
        UserAvatar(
            name = value.name,
            color = experienceColor,
            size = AppTokens.dp.userCard.avatar.small,
            textStyle = AppTokens.typography.h5(),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
        ) {
            Text(
                text = value.name,
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = value.email,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@AppPreview
@Composable
private fun UserCardCompactPreview() {
    PreviewContainer {
        ExperienceEnumState.entries.forEach { experience ->
            UserCardCompact(
                value = stubUser().copy(experience = experience)
            )
        }
    }
}
