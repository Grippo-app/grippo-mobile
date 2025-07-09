package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.date.utils.DateCompose
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.components.user.internal.OverviewCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Calendar
import com.grippo.design.resources.icons.Gym
import com.grippo.design.resources.icons.Trophy
import com.grippo.design.resources.user_card_no_records_yet
import com.grippo.design.resources.user_card_no_workouts_yet
import com.grippo.design.resources.user_card_overview
import com.grippo.design.resources.user_card_value_in_a_row
import com.grippo.design.resources.user_card_value_personal_records
import com.grippo.design.resources.user_card_value_workouts
import com.grippo.presentation.api.profile.models.UserState
import com.grippo.presentation.api.profile.models.stubUser

@Immutable
public sealed interface UserCardStyle {
    @Immutable
    public data object Preview : UserCardStyle

    @Immutable
    public data class Interactive(
        val onEditClick: () -> Unit
    ) : UserCardStyle
}

@Composable
public fun UserCard(
    modifier: Modifier = Modifier,
    value: UserState,
    style: UserCardStyle = UserCardStyle.Preview
) {

    val shape = RoundedCornerShape(AppTokens.dp.userCard.radius)

    Column(
        modifier = modifier
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow
            )
            .clip(shape = shape)
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
            .padding(
                horizontal = AppTokens.dp.userCard.horizontalPadding,
                vertical = AppTokens.dp.userCard.verticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.name,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        BodyItem(
            weight = value.weight,
            height = value.height
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTokens.colors.divider.primary
            )

            Text(
                text = AppTokens.strings.res(Res.string.user_card_overview),
                style = AppTokens.typography.b14Reg(),
                color = AppTokens.colors.text.secondary,
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTokens.colors.divider.primary
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        val workoutsStr = if (value.records > 0) {
            AppTokens.strings.res(
                Res.string.user_card_value_workouts,
                value.records.toString()
            )
        } else {
            AppTokens.strings.res(Res.string.user_card_no_workouts_yet)
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(intrinsicSize = IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {

            OverviewCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = workoutsStr,
                icon = AppTokens.icons.Gym
            )

            val ago = DateCompose.rememberAgo(value.createdAt)

            OverviewCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = AppTokens.strings.res(
                    Res.string.user_card_value_in_a_row,
                    ago
                ),
                icon = AppTokens.icons.Calendar
            )

            val recordsStr = if (value.records > 0) {
                AppTokens.strings.res(
                    Res.string.user_card_value_personal_records,
                    value.records.toString()
                )
            } else {
                AppTokens.strings.res(Res.string.user_card_no_records_yet)
            }

            OverviewCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = recordsStr,
                icon = AppTokens.icons.Trophy
            )
        }
    }
}

@AppPreview
@Composable
private fun UserCardPreview() {
    PreviewContainer {
        UserCard(
            value = stubUser(),
            style = UserCardStyle.Preview
        )

        UserCard(
            value = stubUser(),
            style = UserCardStyle.Interactive(onEditClick = {})
        )
    }
}