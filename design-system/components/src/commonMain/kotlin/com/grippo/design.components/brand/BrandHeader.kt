package com.grippo.design.components.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.GrippoLogo

@Composable
public fun BrandHeader(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    showLogo: Boolean = false,
) {
    val dp = AppTokens.dp.brandHeader
    Column(
        modifier = modifier
            .illustrationBackground(RoundedCornerShape(dp.radius))
            .padding(horizontal = dp.horizontalPadding, vertical = dp.verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showLogo) {
            Image(
                modifier = Modifier.size(dp.logoSize),
                imageVector = AppTokens.icons.GrippoLogo,
                contentDescription = null,
            )
            Spacer(Modifier.size(AppTokens.dp.contentPadding.content))
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(AppTokens.dp.contentPadding.subContent))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = description,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun BrandHeaderPreview() {
    PreviewContainer {
        BrandHeader(
            modifier = Modifier.fillMaxWidth(),
            title = "Your training profile",
            description = "Choose the settings that work for you.",
        )
        BrandHeader(
            modifier = Modifier.fillMaxWidth(),
            title = "Welcome to Grippo",
            description = "Track your training and progress.",
            showLogo = true,
        )
    }
}
