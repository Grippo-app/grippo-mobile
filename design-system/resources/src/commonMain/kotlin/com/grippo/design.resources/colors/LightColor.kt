package com.grippo.design.resources.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.AppColor

public object LightColor : AppColor {
    private val Transparent = Color(0x00000000)
    private val White = Color(0xFFFFFFFF)
    private val Black = Color(0xFF000000)
    private val Primary = Color(0xFF3366FF)
    private val Error = Color(0xFFFF4D4F)
    private val Warning = Color(0xFFFFAA2C)
    private val Success = Color(0xFF3AC86B)

    private val Neutral100 = Color(0xFFFAFAFA)
    private val Neutral200 = Color(0xFFF2F2F2)
    private val Neutral300 = Color(0xFFDCDCDC)
    private val Neutral400 = Color(0xFFBFBFBF)
    private val Neutral500 = Color(0xFF999999)
    private val Neutral700 = Color(0xFF333333)

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = Primary
        override val contentPrimary = White
        override val backgroundPrimaryDisabled = Neutral200
        override val contentPrimaryDisabled = Neutral500

        override val backgroundSecondary = Transparent
        override val contentSecondary = Primary
        override val backgroundSecondaryDisabled = Neutral100
        override val contentSecondaryDisabled = Neutral300

        override val backgroundTertiary = Transparent
        override val contentTertiary = Primary
        override val backgroundTertiaryDisabled = Transparent
        override val contentTertiaryDisabled = Neutral300

        override val contentTransparentDisabled = Neutral300
    }

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = Neutral200
        override val focus = Primary
        override val disabled = Neutral200
        override val error = Error

        override val defaultSecondary = Primary.copy(alpha = 0.3f)
        override val disabledSecondary = Neutral200
        override val defaultTertiary = Neutral300
        override val disabledTertiary = Neutral200
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val default = Neutral700
        override val disabled = Neutral300
        override val accent = Primary
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val background = White
        override val border = Neutral400
        override val placeholder = Neutral400
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

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val background = White
        override val handle = Neutral300
        override val scrim = Color(0x80000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = Black
        override val secondary = Neutral500
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