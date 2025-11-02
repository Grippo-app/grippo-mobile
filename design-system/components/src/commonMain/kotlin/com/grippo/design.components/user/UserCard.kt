package com.grippo.design.components.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.stubUser
import com.grippo.design.components.user.internal.OverviewCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.icons.Gym
import com.grippo.design.resources.provider.user_card_no_workouts_yet
import com.grippo.design.resources.provider.user_card_value_in_a_row
import com.grippo.design.resources.provider.user_card_value_workouts
import com.grippo.toolkit.date.utils.DateCompose

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

    Brush.verticalGradient(
        colors = listOf(
            AppTokens.colors.background.dialog,
            AppTokens.colors.background.card,
        )
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
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
            textAlign = TextAlign.Center,
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

        val workoutsStr = if (value.trainingsCount > 0) {
            AppTokens.strings.res(
                Res.string.user_card_value_workouts,
                value.trainingsCount.toString()
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