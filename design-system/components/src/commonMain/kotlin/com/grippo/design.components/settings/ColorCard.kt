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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public enum class ColorCardStyle {
    Light,
    Dark;

    @Composable
    public fun colorSet(): ImmutableList<Color> {
        return when (this) {
            Light -> persistentListOf(
                AppTokens.colors.themeColors.lightBackground1,
                AppTokens.colors.themeColors.lightBackground2
            )

            Dark -> persistentListOf(
                AppTokens.colors.themeColors.darkBackground1,
                AppTokens.colors.themeColors.darkBackground2
            )
        }
    }

    @Composable
    public fun text(): String {
        return when (this) {
            Light -> AppTokens.strings.res(Res.string.light)
            Dark -> AppTokens.strings.res(Res.string.dark)
        }
    }

    @Composable
    public fun textColor(): Color {
        return when (this) {
            Light -> AppTokens.colors.themeColors.lightText
            Dark -> AppTokens.colors.themeColors.darkText
        }
    }
}

@Composable
public fun ColorCard(
    modifier: Modifier = Modifier,
    style: ColorCardStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.selectableCard.large.radius)

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.border.focus else AppTokens.colors.border.defaultPrimary,
        label = "border"
    )

    val shadowColor by animateColorAsState(
        if (isSelected) AppTokens.colors.overlay.accentShadow else AppTokens.colors.overlay.defaultShadow,
        label = "shadow"
    )

    val colorSet = Brush.verticalGradient(
        colors = style.colorSet()
    )

    Box(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = shadowColor
            )
            .background(colorSet, shape)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = AppTokens.dp.selectableCard.large.horizontalPadding,
                vertical = AppTokens.dp.selectableCard.large.verticalPadding
            ),
    ) {
        Text(
            modifier = Modifier.align(Alignment.TopStart),
            text = style.text(),
            style = AppTokens.typography.h2(),
            color = style.textColor()

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
            style = ColorCardStyle.Light,
            isSelected = true,
            onClick = {}
        )

        ColorCard(
            modifier = Modifier.size(100.dp),
            style = ColorCardStyle.Light,
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
            style = ColorCardStyle.Dark,
            isSelected = true,
            onClick = {}
        )

        ColorCard(
            modifier = Modifier.size(100.dp),
            style = ColorCardStyle.Dark,
            isSelected = false,
            onClick = {}
        )
    }
}