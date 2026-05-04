package com.grippo.design.components.user.internal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun UserCardDetailed(
    modifier: Modifier = Modifier,
    value: UserState,
) {
    val experienceColor = value.experience.color()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(
            name = value.name,
            color = experienceColor,
            size = AppTokens.dp.userCard.avatar.large,
            textStyle = AppTokens.typography.h1(),
            ringWidth = AppTokens.dp.userCard.detailed.avatarRing,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        Text(
            text = value.name,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

        Text(
            text = value.email,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun UserCardDetailedPreview() {
    PreviewContainer {
        ExperienceEnumState.entries.forEach { experience ->
            UserCardDetailed(
                value = stubUser().copy(experience = experience)
            )
        }
    }
}
