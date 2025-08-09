package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.date.utils.DateCompose
import com.grippo.design.components.user.internal.OverviewCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.Calendar
import com.grippo.design.resources.icons.Gym
import com.grippo.design.resources.icons.Trophy
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.user_card_no_records_yet
import com.grippo.design.resources.provider.user_card_no_workouts_yet
import com.grippo.design.resources.provider.user_card_value_in_a_row
import com.grippo.design.resources.provider.user_card_value_personal_records
import com.grippo.design.resources.provider.user_card_value_workouts
import com.grippo.state.profile.UserState
import com.grippo.state.profile.stubUser

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

    val gradient = Brush.verticalGradient(
        colors = listOf(
            AppTokens.colors.background.dialog,
            AppTokens.colors.background.card,
        )
    )

    Column(
        modifier = modifier
            .background(gradient, shape)
            .border(width = 2.dp, color = AppTokens.colors.background.card, shape = shape)
            .padding(
                horizontal = AppTokens.dp.userCard.horizontalPadding,
                vertical = AppTokens.dp.userCard.verticalPadding
            ),
    ) {

        Text(
            text = "\uD83D\uDE0E",
            style = AppTokens.typography.h1(),
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        BodyItem(
            weight = value.weight,
            height = value.height
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = AppTokens.colors.divider.default
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        val workoutsStr = if (value.records > 0) {
            AppTokens.strings.res(
                Res.string.user_card_value_workouts,
                value.records.toString()
            )
        } else {
            AppTokens.strings.res(Res.string.user_card_no_workouts_yet)
        }

        OverviewCard(
            modifier = Modifier.fillMaxWidth(),
            title = workoutsStr,
            icon = AppTokens.icons.Gym
        )

        val ago = DateCompose.rememberAgo(value.createdAt)

        OverviewCard(
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
            title = recordsStr,
            icon = AppTokens.icons.Trophy
        )
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