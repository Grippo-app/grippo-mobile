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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.date.utils.DateCompose
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Award
import com.grippo.design.resources.icons.Box
import com.grippo.design.resources.icons.Calendar
import com.grippo.design.resources.icons.Edit
import com.grippo.design.resources.icons.Heart
import com.grippo.design.resources.user_card_no_records_yet
import com.grippo.design.resources.user_card_no_workouts_yet
import com.grippo.design.resources.user_card_overview
import com.grippo.design.resources.user_card_value_in_a_row
import com.grippo.design.resources.user_card_value_personal_records
import com.grippo.design.resources.user_card_value_workouts
import com.grippo.presentation.api.user.models.UserState
import com.grippo.presentation.api.user.models.stubUser

@Composable
public fun UserCard(
    modifier: Modifier = Modifier,
    value: UserState,
    onEditClick: (() -> Unit)? = null
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
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = value.name,
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (onEditClick != null) {
                Icon(
                    modifier = Modifier
                        .size(AppTokens.dp.icon.m)
                        .nonRippleClick(onClick = onEditClick),
                    imageVector = AppTokens.icons.Edit,
                    contentDescription = null,
                    tint = AppTokens.colors.icon.default
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(AppTokens.dp.shape.medium))
                .background(AppTokens.colors.background.primary)
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
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.paddings.smallHorizontal),
            verticalAlignment = Alignment.CenterVertically
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

        Spacer(modifier = Modifier.height(12.dp))

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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OverviewItem(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = workoutsStr,
                icon = AppTokens.icons.Heart
            )

            val ago = DateCompose.rememberAgo(value.createdAt)

            OverviewItem(
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

            OverviewItem(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = recordsStr,
                icon = AppTokens.icons.Award
            )
        }
    }
}

@Composable
private fun OverviewItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.shape.medium))
            .background(AppTokens.colors.background.primary)
            .border(
                1.dp,
                color = AppTokens.colors.border.defaultPrimary,
                shape = RoundedCornerShape(AppTokens.dp.shape.medium)
            ).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.icon.m),
            imageVector = icon,
            tint = AppTokens.colors.icon.accent,
            contentDescription = null
        )

        Text(
            text = title,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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

        UserCard(
            value = stubUser(),
            onEditClick = {}
        )
    }
}