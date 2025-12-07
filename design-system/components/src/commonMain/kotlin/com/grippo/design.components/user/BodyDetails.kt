package com.grippo.design.components.user

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cm
import com.grippo.design.resources.provider.icons.ExpandLines
import com.grippo.design.resources.provider.icons.Weight
import com.grippo.design.resources.provider.kg

@Composable
internal fun BodyItem(
    modifier: Modifier = Modifier,
    weight: WeightFormatState,
    height: HeightFormatState,
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
            modifier = Modifier.size(AppTokens.dp.bodyDetails.icon),
            imageVector = AppTokens.icons.Weight,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = weight.display,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.kg),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.content))

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = AppTokens.colors.divider.default
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.content))

        Icon(
            modifier = Modifier.size(AppTokens.dp.bodyDetails.icon),
            imageVector = AppTokens.icons.ExpandLines,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = height.display,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.cm),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@AppPreview
@Composable
private fun BodyItemPreview() {
    PreviewContainer {
        BodyItem(
            weight = WeightFormatState.of(100.0f),
            height = HeightFormatState.of(182)
        )
    }
}