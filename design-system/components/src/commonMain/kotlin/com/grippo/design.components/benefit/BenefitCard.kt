package com.grippo.design.components.benefit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Sparkle

@Composable
public fun BenefitCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
) {
    Row(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.benefit.radius)
            )
            .padding(
                horizontal = AppTokens.dp.benefit.horizontalPadding,
                vertical = AppTokens.dp.benefit.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.benefit.space)
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.benefit.iconBackground)
                .background(
                    color = tint.copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.benefit.icon),
                imageVector = icon,
                contentDescription = null,
                tint = tint,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

            Text(
                text = subtitle,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@AppPreview
@Composable
private fun BenefitCardPreview() {
    PreviewContainer {
        BenefitCard(
            modifier = Modifier.fillMaxWidth(),
            title = "A space that knows you",
            subtitle = "The little details help us see what really matters to you in training.",
            icon = AppTokens.icons.Sparkle,
            tint = AppTokens.colors.brand.color1,
        )
    }
}
