package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.components.user.internal.UserAvatar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.profile_name_not_set

@Composable
public fun UserProfileHeader(
    modifier: Modifier = Modifier,
    value: UserState?,
    loading: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .illustrationBackground(RoundedCornerShape(AppTokens.dp.userCard.card.radius))
            .padding(
                horizontal = AppTokens.dp.userCard.card.horizontalPadding,
                vertical = AppTokens.dp.userCard.card.verticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            Box(
                Modifier
                    .size(AppTokens.dp.userCard.avatar.small)
                    .clip(CircleShape)
                    .background(AppTokens.colors.divider.default)
                    .shimmer(),
            )
        } else {
            UserAvatar(
                name = value?.name.orEmpty(),
                color = value?.experience?.color() ?: AppTokens.colors.icon.secondary,
                size = AppTokens.dp.userCard.avatar.small,
                textStyle = AppTokens.typography.h5(),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            if (loading) {
                Box(
                    Modifier
                        .fillMaxWidth(AppTokens.dp.userCard.headerLoading.nameWidthFraction)
                        .height(AppTokens.dp.userCard.headerLoading.nameHeight)
                        .clip(RoundedCornerShape(AppTokens.dp.userCard.headerLoading.radius))
                        .background(AppTokens.colors.divider.default)
                        .shimmer()
                        .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
                )
                Box(
                    Modifier
                        .fillMaxWidth(AppTokens.dp.userCard.headerLoading.emailWidthFraction)
                        .height(AppTokens.dp.userCard.headerLoading.emailHeight)
                        .clip(RoundedCornerShape(AppTokens.dp.userCard.headerLoading.radius))
                        .background(AppTokens.colors.divider.default)
                        .shimmer(),
                )
            } else {
                val name = value?.name?.takeIf { it.isNotBlank() }
                Text(
                    text = name ?: AppTokens.strings.res(Res.string.profile_name_not_set),
                    style = AppTokens.typography.h3(),
                    color = if (name == null) AppTokens.colors.text.tertiary else AppTokens.colors.text.primary,
                )
                value?.email?.takeIf { it.isNotBlank() }?.let { email ->
                    Text(
                        text = email,
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun UserProfileHeaderPreview() {
    PreviewContainer {
        UserProfileHeader(value = stubUser().copy(name = "Alex", email = "alex@grippo.app"))
    }
}

@AppPreview
@Composable
private fun UserProfileHeaderEmptyPreview() {
    PreviewContainer {
        UserProfileHeader(value = null)
    }
}

@AppPreview
@Composable
private fun UserProfileHeaderLoadingPreview() {
    PreviewContainer {
        UserProfileHeader(value = null, loading = true)
    }
}

@AppPreview
@Composable
private fun UserProfileHeaderPartialPreview() {
    PreviewContainer {
        UserProfileHeader(value = stubUser().copy(name = "", email = "alex@grippo.app"))
        UserProfileHeader(value = stubUser().copy(name = "Alex", email = ""))
    }
}

@AppPreview
@Composable
private fun UserProfileHeaderLongContentPreview() {
    PreviewContainer {
        UserProfileHeader(
            value = stubUser().copy(
                name = "Олександр Костянтинович",
                email = "oleksandr.kostiantynovych@grippo.app",
            ),
        )
    }
}
