package com.grippo.design.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.components.toggle.Toggle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.dark
import com.grippo.design.resources.light
import com.grippo.presentation.api.settings.models.ColorModeState
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun ColorCard(
    modifier: Modifier = Modifier,
    style: ColorModeState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.selectableCard.large.radius)

    val background = when (style) {
        ColorModeState.LIGHT -> persistentListOf(
            AppTokens.colors.themeColors.lightBackground1,
            AppTokens.colors.themeColors.lightBackground2
        )

        ColorModeState.DARK -> persistentListOf(
            AppTokens.colors.themeColors.darkBackground1,
            AppTokens.colors.themeColors.darkBackground2
        )
    }

    val text = when (style) {
        ColorModeState.LIGHT -> AppTokens.strings.res(Res.string.light)
        ColorModeState.DARK -> AppTokens.strings.res(Res.string.dark)
    }

    val textColor = when (style) {
        ColorModeState.LIGHT -> AppTokens.colors.themeColors.lightText
        ColorModeState.DARK -> AppTokens.colors.themeColors.darkText
    }

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.border.focus else AppTokens.colors.border.defaultPrimary,
        label = "border"
    )

    val shadowColor by animateColorAsState(
        if (isSelected) AppTokens.colors.overlay.accentShadow else AppTokens.colors.overlay.defaultShadow,
        label = "shadow"
    )

    Box(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = shadowColor
            )
            .background(Brush.verticalGradient(colors = background), shape)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = AppTokens.dp.selectableCard.large.horizontalPadding,
                vertical = AppTokens.dp.selectableCard.large.verticalPadding
            ),
    ) {
        Text(
            modifier = Modifier.align(Alignment.TopStart),
            text = text,
            style = AppTokens.typography.h2(),
            color = textColor
        )

        Toggle(
            modifier = Modifier.align(Alignment.BottomEnd),
            checked = isSelected,
            onCheckedChange = onClick
        )
    }
}

@AppPreview
@Composable
private fun ColorCardLightPreview() {
    PreviewContainer {
        ColorCard(
            modifier = Modifier.size(100.dp),
            style = ColorModeState.LIGHT,
            isSelected = true,
            onClick = {}
        )

        ColorCard(
            modifier = Modifier.size(100.dp),
            style = ColorModeState.LIGHT,
            isSelected = false,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ColorCardDarkPreview() {
    PreviewContainer {
        ColorCard(
            modifier = Modifier.size(100.dp),
            style = ColorModeState.DARK,
            isSelected = true,
            onClick = {}
        )

        ColorCard(
            modifier = Modifier.size(100.dp),
            style = ColorModeState.DARK,
            isSelected = false,
            onClick = {}
        )
    }
}