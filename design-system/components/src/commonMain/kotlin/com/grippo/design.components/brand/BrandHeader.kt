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
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.GrippoLogo
import com.grippo.design.resources.provider.login_title
import com.grippo.design.resources.provider.registration_description
import com.grippo.design.resources.provider.registration_equipment_description
import com.grippo.design.resources.provider.registration_equipment_title
import com.grippo.design.resources.provider.registration_title
import com.grippo.design.resources.provider.track_real_progress_no_fluff

@Composable
public fun BrandHeader(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    showLogo: Boolean = false,
    style: BrandHeaderStyle = BrandHeaderStyle.Default,
) {
    val dp = AppTokens.dp.brandHeader
    val isCompact = style == BrandHeaderStyle.Compact
    val textAlign = if (isCompact) TextAlign.Start else TextAlign.Center

    Column(
        modifier = modifier
            .illustrationBackground(RoundedCornerShape(dp.radius))
            .padding(
                horizontal = dp.horizontalPadding,
                vertical = if (isCompact) AppTokens.dp.contentPadding.content else dp.verticalPadding,
            ),
        horizontalAlignment = if (isCompact) Alignment.Start else Alignment.CenterHorizontally,
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
            style = if (isCompact) AppTokens.typography.h3() else AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = textAlign,
        )
        Spacer(Modifier.size(AppTokens.dp.contentPadding.subContent))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = description,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = textAlign,
        )
    }
}

@AppPreview
@Composable
private fun BrandHeaderDefaultPreview() {
    PreviewContainer {
        BrandHeader(
            modifier = Modifier.fillMaxWidth(),
            title = AppTokens.strings.res(Res.string.registration_title),
            description = AppTokens.strings.res(Res.string.registration_description),
        )
    }
}

@AppPreview
@Composable
private fun BrandHeaderWithLogoPreview() {
    PreviewContainer {
        BrandHeader(
            modifier = Modifier.fillMaxWidth(),
            title = AppTokens.strings.res(Res.string.login_title),
            description = AppTokens.strings.res(Res.string.track_real_progress_no_fluff),
            showLogo = true,
        )
    }
}

@AppPreview
@Composable
private fun BrandHeaderCompactPreview() {
    PreviewContainer {
        BrandHeader(
            modifier = Modifier.fillMaxWidth(),
            title = AppTokens.strings.res(Res.string.registration_equipment_title),
            description = AppTokens.strings.res(Res.string.registration_equipment_description),
            style = BrandHeaderStyle.Compact,
        )
    }
}
