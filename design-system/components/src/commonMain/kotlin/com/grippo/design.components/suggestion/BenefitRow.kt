package com.grippo.design.components.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Sparkle

@Composable
public fun BenefitRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.metrics.profile.goal.benefit.radius),
            )
            .padding(
                horizontal = AppTokens.dp.metrics.profile.goal.benefit.horizontalPadding,
                vertical = AppTokens.dp.metrics.profile.goal.benefit.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.metrics.profile.goal.benefit.iconBackground)
                .background(
                    color = AppTokens.colors.brand.color1.copy(alpha = 0.18f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.profile.goal.benefit.icon),
                imageVector = icon,
                tint = AppTokens.colors.brand.color1,
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = title,
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )

            Text(
                text = description,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@AppPreview
@Composable
private fun BenefitRowPreview() {
    PreviewContainer {
        BenefitRow(
            modifier = Modifier.fillMaxWidth(),
            icon = AppTokens.icons.Sparkle,
            title = "A space that knows you",
            description = "The little details help us see what really matters to you in training.",
        )
    }
}
