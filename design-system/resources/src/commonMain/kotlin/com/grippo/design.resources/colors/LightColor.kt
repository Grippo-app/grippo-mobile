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
    private val PrimaryLight = Color(0xFFB3D4FC)
    private val PrimaryDark = Color(0xFF2A6AB1)

    private val Error = Color(0xFFFF6B6B)
    private val Warning = Color(0xFFFFC107)
    private val Success = Color(0xFF4CAF50)
    private val Info = Color(0xFF03A9F4)

    private val Transparent = Color(0x00000000)

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val primaryBackground = Primary
        override val primaryContent = White
        override val primaryDisabledBackground = Neutral200
        override val primaryDisabledContent = Neutral500

        override val secondaryBackground = Neutral200
        override val secondaryContent = Primary
        override val secondaryDisabledBackground = Neutral100
        override val secondaryDisabledContent = Neutral300

        override val tertiaryBackground = Transparent
        override val tertiaryContent = Primary
        override val tertiaryDisabledContent = Neutral300
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val background = White
        override val border = Neutral300
        override val placeholder = Neutral500
        override val text = Black

        override val disabledBackground = Neutral100
        override val disabledBorder = Neutral200
        override val disabledText = Neutral300
        override val disabledPlaceholder = Neutral300
    }

    override val state: AppColor.StateColors = object : AppColor.StateColors {
        override val error = Error
        override val success = Success
        override val warning = Warning

        override val onError = White
        override val onSuccess = White
        override val onWarning = Black
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

    override val divider: Color = Neutral200

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = Success
        override val error = Error
        override val warning = Warning
        override val info = Info
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val level1 = Color(0x14000000) // 8%
        override val level2 = Color(0x29000000) // 16%
        override val level3 = Color(0x3D000000) // 24%
        override val level4 = Color(0x52000000) // 32%
    }

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = Neutral300
        override val focus = Primary
        override val disabled = Neutral200
        override val error = Error
    }

    override val interaction: AppColor.InteractionColors = object : AppColor.InteractionColors {
        override val pressed = PrimaryDark.copy(alpha = 0.12f)
        override val hovered = Primary.copy(alpha = 0.08f)
        override val focused = Primary.copy(alpha = 0.20f)
    }

    override val elevation: AppColor.ElevationColors = object : AppColor.ElevationColors {
        override val level0 = White
        override val level1 = Neutral100
        override val level2 = Neutral200
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val background = Neutral200
        override val shimmer = White.copy(alpha = 0.6f)
    }
}