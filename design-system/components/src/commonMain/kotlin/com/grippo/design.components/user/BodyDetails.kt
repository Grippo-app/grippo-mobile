package com.grippo.design.components.user

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cm
import com.grippo.design.resources.provider.icons.ExpandLines
import com.grippo.design.resources.provider.icons.Weight
import com.grippo.design.resources.provider.kg

@Composable
internal fun BodyItem(
    modifier: Modifier = Modifier,
    weight: Float,
    height: Int,
) {
    Row(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Min)
            .padding(
                vertical = AppTokens.dp.bodyDetails.verticalPadding,
                horizontal = AppTokens.dp.bodyDetails.horizontalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(4.dp),
            imageVector = AppTokens.icons.Weight,
            tint = AppTokens.colors.icon.primary,
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
            modifier = Modifier.padding(4.dp),
            imageVector = AppTokens.icons.ExpandLines,
            tint = AppTokens.colors.icon.primary,
            contentDescription = null
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        Text(
            text = height.toString(),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = AppTokens.strings.res(Res.string.cm),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(8.dp))
    }
}