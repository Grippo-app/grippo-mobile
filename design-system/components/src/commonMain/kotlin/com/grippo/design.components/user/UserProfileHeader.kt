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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.components.user.internal.UserAvatar
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.profile_name_not_set
import com.grippo.design.resources.provider.profile_settings_identity_unavailable
import com.grippo.design.resources.provider.profile_settings_signed_in_as

@Immutable
public sealed interface UserProfileHeaderStyle {
    @Immutable
    public data class Profile(val loading: Boolean = false) : UserProfileHeaderStyle

    @Immutable
    public data object Account : UserProfileHeaderStyle
}

@Composable
public fun UserProfileHeader(
    modifier: Modifier = Modifier,
    value: UserState?,
    style: UserProfileHeaderStyle = UserProfileHeaderStyle.Profile(),
) {
    when (style) {
        is UserProfileHeaderStyle.Profile -> ProfileHeader(
            modifier = modifier,
            value = value,
            loading = style.loading,
        )

        UserProfileHeaderStyle.Account -> AccountHeader(
            modifier = modifier,
            value = value,
        )
    }
}

@Composable
private fun ProfileHeader(
    modifier: Modifier = Modifier,
    value: UserState?,
    loading: Boolean,
) {
    val nameStyle = AppTokens.typography.h3()
    val emailStyle = AppTokens.typography.b13Med()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .illustrationBackground(shape = RoundedCornerShape(AppTokens.dp.userCard.card.radius))
            .padding(
                horizontal = AppTokens.dp.userCard.card.horizontalPadding,
                vertical = AppTokens.dp.userCard.card.verticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            Box(
                modifier = Modifier
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
                val textMeasurer = rememberTextMeasurer()
                val nameHeight = rememberTextHeight(style = nameStyle, textMeasurer = textMeasurer)
                val emailHeight = rememberTextHeight(style = emailStyle, textMeasurer = textMeasurer)

                Box(
                    modifier = Modifier
                        .fillMaxWidth(AppTokens.dp.userCard.headerLoading.nameWidthFraction)
                        .height(nameHeight)
                        .clip(RoundedCornerShape(AppTokens.dp.userCard.headerLoading.radius))
                        .background(AppTokens.colors.divider.default)
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(AppTokens.dp.userCard.headerLoading.emailWidthFraction)
                        .height(emailHeight)
                        .clip(RoundedCornerShape(AppTokens.dp.userCard.headerLoading.radius))
                        .background(AppTokens.colors.divider.default),
                )
            } else {
                val name = value?.name?.takeIf { it.isNotBlank() }
                val email = value?.email?.takeIf { it.isNotBlank() }

                Text(
                    text = name ?: AppTokens.strings.res(Res.string.profile_name_not_set),
                    style = nameStyle,
                    color = if (name == null) {
                        AppTokens.colors.text.tertiary
                    } else {
                        AppTokens.colors.text.primary
                    },
                )
                if (email != null) {
                    Text(
                        text = email,
                        style = emailStyle,
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeader(
    modifier: Modifier = Modifier,
    value: UserState?,
) {
    val name = value?.name?.takeIf { it.isNotBlank() }
    val email = value?.email?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.userCard.card.radius),
            )
            .padding(AppTokens.dp.menu.contentPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.profile_settings_signed_in_as),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
        if (name == null && email == null) {
            Text(
                text = AppTokens.strings.res(Res.string.profile_settings_identity_unavailable),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
            )
        } else {
            if (name != null) {
                Text(
                    text = name,
                    style = AppTokens.typography.h5(),
                    color = AppTokens.colors.text.primary,
                )
            }
            if (email != null) {
                Text(
                    text = email,
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun UserProfileHeaderPreview() {
    PreviewContainer {
        UserProfileHeader(
            value = stubUser().copy(name = "Alex", email = "alex@grippo.app"),
        )
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
        UserProfileHeader(value = null, style = UserProfileHeaderStyle.Profile(loading = true))
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

@AppPreview
@Composable
private fun UserAccountHeaderPreview() {
    PreviewContainer {
        UserProfileHeader(
            value = stubUser().copy(name = "Alex", email = "alex@grippo.app"),
            style = UserProfileHeaderStyle.Account,
        )
    }
}

@AppPreview
@Composable
private fun UserAccountHeaderUnavailablePreview() {
    PreviewContainer {
        UserProfileHeader(value = null, style = UserProfileHeaderStyle.Account)
    }
}

@AppPreview
@Composable
private fun UserAccountHeaderPartialPreview() {
    PreviewContainer {
        UserProfileHeader(
            value = stubUser().copy(name = "", email = "alex@grippo.app"),
            style = UserProfileHeaderStyle.Account,
        )
        UserProfileHeader(
            value = stubUser().copy(name = "Alex", email = ""),
            style = UserProfileHeaderStyle.Account,
        )
    }
}

@AppPreview
@Composable
private fun UserAccountHeaderLongContentPreview() {
    PreviewContainer {
        UserProfileHeader(
            value = stubUser().copy(
                name = "Олександр Костянтинович Петренко",
                email = "oleksandr.kostiantynovych.petrenko@athlete.grippo.app",
            ),
            style = UserProfileHeaderStyle.Account,
        )
    }
}
