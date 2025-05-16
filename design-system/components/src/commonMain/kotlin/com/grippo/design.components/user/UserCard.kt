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
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.Award
import com.grippo.design.resources.icons.Calendar
import com.grippo.design.resources.icons.Heart
import com.grippo.design.resources.user_card_overview
import com.grippo.design.resources.user_card_preferences

@Composable
public fun UserCard(
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppTokens.dp.shape.large)

    Column(
        modifier = modifier
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow
            ).clip(shape = shape)
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
            text = "Max Voitenko",
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
                .fillMaxWidth(0.5f)
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
            Text(
                modifier = Modifier.weight(1f),
                text = "80 kg",
                style = AppTokens.typography.b15Bold(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = AppTokens.colors.divider.default
            )

            Text(
                modifier = Modifier.weight(1f),
                text = "175 cm",
                style = AppTokens.typography.b15Bold(),
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

        OverviewItem(
            modifier = Modifier.fillMaxWidth(0.7f),
            title = "125 Workouts",
            icon = AppTokens.icons.Heart
        )

        Spacer(modifier = Modifier.height(12.dp))

        OverviewItem(
            modifier = Modifier.fillMaxWidth(0.7f),
            title = "16 Days in a Row",
            icon = AppTokens.icons.Calendar
        )

        Spacer(modifier = Modifier.height(12.dp))

        OverviewItem(
            modifier = Modifier.fillMaxWidth(0.7f),
            title = "8 Personal Records",
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
                .size(AppTokens.dp.icon.medium),
            imageVector = icon,
            tint = AppTokens.colors.icon.accent,
            contentDescription = null
        )

        Text(
            text = title,
            style = AppTokens.typography.b15Bold(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )
    }
}