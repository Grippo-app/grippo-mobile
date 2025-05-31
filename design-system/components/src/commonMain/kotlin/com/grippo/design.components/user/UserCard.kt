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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.date.utils.DateCompose
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Award
import com.grippo.design.resources.icons.Box
import com.grippo.design.resources.icons.Calendar
import com.grippo.design.resources.icons.Heart
import com.grippo.design.resources.user_card_no_records_yet
import com.grippo.design.resources.user_card_no_workouts_yet
import com.grippo.design.resources.user_card_overview
import com.grippo.design.resources.user_card_preferences
import com.grippo.design.resources.user_card_value_in_a_row
import com.grippo.design.resources.user_card_value_personal_records
import com.grippo.design.resources.user_card_value_workouts
import com.grippo.presentation.api.user.models.UserState
import com.grippo.presentation.api.user.models.stubUser

@Composable
public fun UserCard(
    modifier: Modifier = Modifier,
    value: UserState
) {
    val shape = RoundedCornerShape(AppTokens.dp.shape.large)

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
                horizontal = AppTokens.dp.paddings.mediumHorizontal,
                vertical = AppTokens.dp.paddings.mediumVertical
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTokens.colors.divider.default
            )

            Text(
                text = AppTokens.strings.res(Res.string.user_card_preferences),
                style = AppTokens.typography.b14Reg(),
                color = AppTokens.colors.text.secondary,
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTokens.colors.divider.default
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .border(
                    1.dp,
                    color = AppTokens.colors.border.defaultPrimary,
                    shape = RoundedCornerShape(AppTokens.dp.shape.medium)
                )
                .height(intrinsicSize = IntrinsicSize.Min)
                .padding(
                    vertical = AppTokens.dp.paddings.smallVertical,
                    horizontal = AppTokens.dp.paddings.smallHorizontal
                ),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.paddings.smallHorizontal)
        ) {

            Icon(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppTokens.dp.shape.small))
                    .background(AppTokens.colors.background.accent)
                    .padding(4.dp)
                    .size(AppTokens.dp.icon.xs),
                imageVector = AppTokens.icons.Box,
                tint = AppTokens.colors.icon.invert,
                contentDescription = null
            )

            Text(
                text = "80 kg",
                style = AppTokens.typography.b16Bold(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = AppTokens.colors.divider.default
            )

            Icon(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppTokens.dp.shape.small))
                    .background(AppTokens.colors.background.accent)
                    .padding(4.dp)
                    .size(AppTokens.dp.icon.xs),
                imageVector = AppTokens.icons.Box,
                tint = AppTokens.colors.icon.invert,
                contentDescription = null
            )

            Text(
                modifier = Modifier,
                text = "175 cm",
                style = AppTokens.typography.b16Bold(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTokens.colors.divider.default
            )

            Text(
                text = AppTokens.strings.res(Res.string.user_card_overview),
                style = AppTokens.typography.b14Reg(),
                color = AppTokens.colors.text.secondary,
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTokens.colors.divider.default
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val workoutsStr = if (value.records > 0) {
            AppTokens.strings.res(
                Res.string.user_card_value_workouts,
                value.records.toString()
            )
        } else {
            AppTokens.strings.res(Res.string.user_card_no_workouts_yet)
        }

        OverviewItem(
            modifier = Modifier.fillMaxWidth(0.7f),
            title = workoutsStr,
            icon = AppTokens.icons.Heart
        )

        Spacer(modifier = Modifier.height(12.dp))

        val ago = DateCompose.rememberAgo(value.createdAt)

        OverviewItem(
            modifier = Modifier.fillMaxWidth(0.7f),
            title = AppTokens.strings.res(
                Res.string.user_card_value_in_a_row,
                ago
            ),
            icon = AppTokens.icons.Calendar
        )

        Spacer(modifier = Modifier.height(12.dp))

        val recordsStr = if (value.records > 0) {
            AppTokens.strings.res(
                Res.string.user_card_value_personal_records,
                value.records.toString()
            )
        } else {
            AppTokens.strings.res(Res.string.user_card_no_records_yet)
        }

        OverviewItem(
            modifier = Modifier.fillMaxWidth(0.7f),
            title = recordsStr,
            icon = AppTokens.icons.Award
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun OverviewItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .border(
                    1.dp,
                    color = AppTokens.colors.border.defaultPrimary,
                    shape = RoundedCornerShape(AppTokens.dp.shape.medium)
                )
                .padding(8.dp)
                .size(AppTokens.dp.icon.m),
            imageVector = icon,
            tint = AppTokens.colors.icon.accent,
            contentDescription = null
        )

        Text(
            text = title,
            style = AppTokens.typography.b16Bold(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )
    }
}

@AppPreview
@Composable
private fun UserCardPreview() {
    PreviewContainer {
        UserCard(
            value = stubUser()
        )
    }
}