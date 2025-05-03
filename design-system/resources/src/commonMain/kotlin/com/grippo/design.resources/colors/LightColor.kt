package com.grippo.design.resources.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.AppColor

public object LightColor : AppColor {
    private val White = Color(0xFFFFFFFF)
    private val Black = Color(0xFF000000)
    private val Neutral100 = Color(0xFFF9F9F9)
    private val Neutral200 = Color(0xFFF0F0F0)
    private val Neutral300 = Color(0xFFE0E0E0)
    private val Neutral500 = Color(0xFF9E9E9E)
    private val Neutral700 = Color(0xFF616161)
    private val Primary = Color(0xFF4A90E2)
    private val Error = Color(0xFFFF6B6B)
    private val Warning = Color(0xFFFFC107)
    private val Success = Color(0xFF4CAF50)
    private val Transparent = Color(0x00000000)

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = Primary
        override val contentPrimary = White
        override val backgroundPrimaryDisabled = Neutral200
        override val contentPrimaryDisabled = Neutral500

        override val backgroundSecondary = Neutral200
        override val contentSecondary = Primary
        override val backgroundSecondaryDisabled = Neutral100
        override val contentSecondaryDisabled = Neutral300

        override val backgroundTertiary = Transparent
        override val contentTertiary = Primary
        override val backgroundTertiaryDisabled = Transparent
        override val contentTertiaryDisabled = Neutral300

        override val contentTransparentDisabled = Neutral300
        override val backgroundBlur = Color(0x40FFFFFF)
    }

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = Neutral300
        override val focus = Primary
        override val disabled = Neutral200
        override val error = Error

        override val defaultSecondary = Neutral300
        override val disabledSecondary = Neutral200
        override val defaultTertiary = Neutral300
        override val disabledTertiary = Neutral200
        override val blur = Color(0x33FFFFFF)
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val default = Neutral700
        override val disabled = Neutral300
        override val accent = Primary
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val background = White
        override val border = Neutral300
        override val placeholder = Neutral500
        override val text = Black

        override val backgroundDisabled = Neutral100
        override val borderDisabled = Neutral200
        override val textDisabled = Neutral300
        override val placeholderDisabled = Neutral300
    }

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val primary = White
        override val secondary = Neutral100
    }

    override val bottomSheet: AppColor.BottomSheetColors = object : AppColor.BottomSheetColors {
        override val background = White
        override val handle = Neutral300
        override val scrim = Color(0x80000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = Black
        override val secondary = Neutral700
        override val disabled = Neutral300
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = Success
        override val error = Error
        override val warning = Warning
        override val info = Primary
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val level1 = Color(0x0D000000)
        override val level2 = Color(0x1A000000)
        override val level3 = Color(0x33000000)
        override val level4 = Color(0x4D000000)
    }

    override val interaction: AppColor.InteractionColors = object : AppColor.InteractionColors {
        override val pressed = Neutral200
        override val hovered = Neutral100
        override val focused = Primary
    }

    override val elevation: AppColor.ElevationColors = object : AppColor.ElevationColors {
        override val level0 = Color(0x00000000)
        override val level1 = Color(0x05000000)
        override val level2 = Color(0x0A000000)
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val background = Neutral200
        override val shimmer = Neutral100
    }

    override val divider: Color = Neutral200
}