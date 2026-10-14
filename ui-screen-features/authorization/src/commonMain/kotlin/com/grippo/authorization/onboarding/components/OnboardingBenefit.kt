package com.grippo.authorization.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens

@Composable
internal fun OnboardingBenefit(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(AppTokens.colors.background.card, RoundedCornerShape(AppTokens.dp.focusFrame.radius))
                .padding(AppTokens.dp.contentPadding.content),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.bannerCard.icon),
                imageVector = icon,
                contentDescription = null,
                tint = AppTokens.colors.icon.secondary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(title, style = AppTokens.typography.h6(), color = AppTokens.colors.text.primary)
            Text(description, style = AppTokens.typography.b13Med(), color = AppTokens.colors.text.secondary)
        }
    }
}
