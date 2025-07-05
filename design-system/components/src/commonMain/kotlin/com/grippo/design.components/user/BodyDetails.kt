package com.grippo.design.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.cm
import com.grippo.design.resources.icons.Box
import com.grippo.design.resources.kg

@Composable
internal fun BodyItem(
    modifier: Modifier = Modifier,
    weight: Float,
    height: Int,
) {
    Row(
        modifier = modifier
            .clip(shape = RoundedCornerShape(AppTokens.dp.bodyDetails.radius))
            .background(AppTokens.colors.background.primary)
            .border(
                1.dp,
                color = AppTokens.colors.border.defaultPrimary,
                shape = RoundedCornerShape(AppTokens.dp.bodyDetails.radius)
            )
            .height(intrinsicSize = IntrinsicSize.Min)
            .padding(
                vertical = AppTokens.dp.bodyDetails.verticalPadding,
                horizontal = AppTokens.dp.bodyDetails.horizontalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            modifier = Modifier
                .clip(RoundedCornerShape(AppTokens.dp.bodyDetails.radius))
                .background(AppTokens.colors.background.accent)
                .padding(4.dp)
                .size(AppTokens.dp.bodyDetails.icon),
            imageVector = AppTokens.icons.Box,
            tint = AppTokens.colors.icon.invert,
            contentDescription = null
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        Text(
            text = weight.toString(),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = AppTokens.strings.res(Res.string.kg),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = AppTokens.colors.divider.default
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        Icon(
            modifier = Modifier
                .clip(RoundedCornerShape(AppTokens.dp.bodyDetails.radius))
                .background(AppTokens.colors.background.accent)
                .padding(4.dp)
                .size(AppTokens.dp.bodyDetails.icon),
            imageVector = AppTokens.icons.Box,
            tint = AppTokens.colors.icon.invert,
            contentDescription = null
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier,
            text = height.toString(),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier,
            text = AppTokens.strings.res(Res.string.cm),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(8.dp))
    }
}