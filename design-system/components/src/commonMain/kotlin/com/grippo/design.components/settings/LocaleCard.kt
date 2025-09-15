package com.grippo.design.components.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.FlagUa
import com.grippo.state.settings.LocaleState

@Composable
public fun LocaleCard(
    modifier: Modifier = Modifier,
    value: LocaleState,
    onClick: () -> Unit
) {

    val shape = RoundedCornerShape(AppTokens.dp.localeCard.radius)


    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.localeCard.horizontalPadding,
                vertical = AppTokens.dp.localeCard.verticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(AppTokens.dp.localeCard.icon),
            imageVector = AppTokens.icons.FlagUa,
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

        Text(
            text = value.title().text(),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary
        )
    }
}

@AppPreview
@Composable
private fun LocaleCardPreview() {
    PreviewContainer {
        LocaleCard(
            value = LocaleState.UA,
            onClick = {}
        )
    }
}