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
import com.grippo.design.components.selectors.Radio
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.dark
import com.grippo.design.resources.light
import com.grippo.presentation.api.settings.models.ThemeState
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun ThemeCard(
    modifier: Modifier = Modifier,
    style: ThemeState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.themeCard.radius)

    val background = when (style) {
        ThemeState.LIGHT -> persistentListOf(
            AppTokens.colors.theme.lightBackground1,
            AppTokens.colors.theme.lightBackground2
        )

        ThemeState.DARK -> persistentListOf(
            AppTokens.colors.theme.darkBackground1,
            AppTokens.colors.theme.darkBackground2
        )
    }

    val text = when (style) {
        ThemeState.LIGHT -> AppTokens.strings.res(Res.string.light)
        ThemeState.DARK -> AppTokens.strings.res(Res.string.dark)
    }

    val textColor = when (style) {
        ThemeState.LIGHT -> AppTokens.colors.theme.lightText
        ThemeState.DARK -> AppTokens.colors.theme.darkText
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
                horizontal = AppTokens.dp.themeCard.horizontalPadding,
                vertical = AppTokens.dp.themeCard.verticalPadding
            ),
    ) {
        Text(
            modifier = Modifier.align(Alignment.TopStart),
            text = text,
            style = AppTokens.typography.h2(),
            color = textColor
        )

        Radio(
            modifier = Modifier.align(Alignment.BottomEnd),
            selected = isSelected,
            onSelectedChange = onClick
        )
    }
}

@AppPreview
@Composable
private fun ThemeCardLightPreview() {
    PreviewContainer {
        ThemeCard(
            modifier = Modifier.size(100.dp),
            style = ThemeState.LIGHT,
            isSelected = true,
            onClick = {}
        )

        ThemeCard(
            modifier = Modifier.size(100.dp),
            style = ThemeState.LIGHT,
            isSelected = false,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ThemeCardDarkPreview() {
    PreviewContainer {
        ThemeCard(
            modifier = Modifier.size(100.dp),
            style = ThemeState.DARK,
            isSelected = true,
            onClick = {}
        )

        ThemeCard(
            modifier = Modifier.size(100.dp),
            style = ThemeState.DARK,
            isSelected = false,
            onClick = {}
        )
    }
}