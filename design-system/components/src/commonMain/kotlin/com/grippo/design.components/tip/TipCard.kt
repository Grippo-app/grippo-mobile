package com.grippo.design.components.tip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.advice
import com.grippo.design.resources.provider.icons.Warning

@Composable
public fun TipCard(
    modifier: Modifier = Modifier,
    value: String,
) {
    Row(
        modifier = modifier
            .background(
                AppTokens.colors.semantic.success.copy(alpha = 0.25f),
                RoundedCornerShape(AppTokens.dp.tip.radius)
            )
            .padding(
                horizontal = AppTokens.dp.tip.horizontalPadding,
                vertical = AppTokens.dp.tip.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.tip.image),
            imageVector = AppTokens.icons.Warning,
            tint = AppTokens.colors.semantic.success,
            contentDescription = null
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
        ) {

            Text(
                text = AppTokens.strings.res(Res.string.advice),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.semantic.success,
            )

            Text(
                text = value,
                style = AppTokens.typography.b13Semi(),
                color = AppTokens.colors.text.primary,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingLoadProfileDetailsCardPreview() {
    PreviewContainer {
        TipCard(
            value = "My test tip",
        )
    }
}
