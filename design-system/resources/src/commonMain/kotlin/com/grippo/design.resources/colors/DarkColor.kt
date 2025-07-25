package com.grippo.design.resources.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.AppColor
import com.grippo.design.resources.AppColor.DividerColors

public object DarkColor : AppColor {
    private val White = Color(0xFFFFFFFF)
    private val Black = Color(0xFF000000)
    private val Error = Color(0xFFFF6B6D)
    private val Warning = Color(0xFFFFBF5C)
    private val Success = Color(0xFF5AD987)

    // Keep primary colors similar but slightly adjusted for dark theme
    private val Primary100 = Color(0xFF1A2747)
    private val Primary150 = Color(0xFF1F2E56)
    private val Primary200 = Color(0xFF243566)
    private val Primary300 = Color(0xFF2D4280)
    private val Primary400 = Color(0xFF3A5099)
    private val Primary500 = Color(0xFF4A66CC)
    private val Primary600 = Color(0xFF5A7AE6)
    private val Primary700 = Color(0xFF7A94F2)
    private val Primary800 = Color(0xFF9AADFF)
    private val Primary900 = Color(0xFFB8C7FF)

    // Invert neutral colors for dark theme
    private val Neutral100 = Color(0xFF121212)
    private val Neutral150 = Color(0xFF181818)
    private val Neutral200 = Color(0xFF202020)
    private val Neutral250 = Color(0xFF252525)
    private val Neutral300 = Color(0xFF303030)
    private val Neutral400 = Color(0xFF404040)
    private val Neutral500 = Color(0xFF707070)
    private val Neutral600 = Color(0xFFA0A0A0)
    private val Neutral700 = Color(0xFFD0D0D0)

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = Primary600
        override val textPrimary = White
        override val iconPrimary = White
        override val backgroundPrimaryDisabled = Neutral300
        override val contentPrimaryDisabled = Neutral600

        override val backgroundSecondary = Neutral300
        override val textSecondary = White
        override val iconSecondary = Primary600
        override val borderSecondary = Primary300
        override val backgroundSecondaryDisabled = Neutral300
        override val contentSecondaryDisabled = Neutral500

        override val textTransparent: Color = Primary600
        override val iconTransparent: Color = Primary600
        override val contentTransparentDisabled = Neutral500
    }

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val defaultPrimary = Neutral400
        override val disabledPrimary = Neutral300
        override val defaultSecondary = Primary300
        override val disabledSecondary = Neutral300
        override val inverted = Black

        override val focus = Primary600
        override val error = Error
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = Neutral700
        override val secondary = Neutral600
        override val disabled = Neutral400
        override val accent = Primary600
        override val inverted = Black
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = White
        override val checkedTrack = Primary600

        override val uncheckedThumb = White
        override val uncheckedTrack = Neutral400

        override val disabledCheckedThumb = Neutral600
        override val disabledCheckedTrack = Neutral300

        override val disabledUncheckedThumb = Neutral500
        override val disabledUncheckedTrack = Neutral300
        override val disabledUncheckedBorder = Neutral400
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val background = Neutral200
        override val border = Neutral500
        override val placeholder = Neutral500
        override val label = Neutral600
        override val text = White

        override val leading: Color = Neutral700
        override val trailing: Color = Neutral700

        override val backgroundDisabled = Neutral300
        override val borderDisabled = Neutral400
        override val textDisabled = Neutral500
        override val placeholderDisabled = Neutral400
    }

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val primary = Neutral100
        override val secondary = Neutral150
        override val accent = Primary600
        override val tertiary = Neutral200
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val background = Neutral150
        override val handle = Neutral600
        override val scrim = Color(0xB3000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = White
        override val secondary = Neutral700
        override val tertiary = Neutral600
        override val inverted = Black
        override val disabled = Neutral500
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = Success
        override val error = Error
        override val warning = Warning
        override val info = Primary600
        override val accent = Primary600
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow: Color = Color(0x40000000)
        override val accentShadow: Color = Primary600.copy(alpha = 0.35f)
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val background = Neutral300
        override val shimmer = Neutral500
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val active = White
        override val inactive: Color = Neutral600
        override val selector: Color = Primary600
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val focused = Primary600
        override val active = Primary500
        override val inactive = Neutral400
        override val background = Neutral300
        override val outline = Neutral400
        override val palette: List<Color> = listOf(
            Color(0xFFEF9A9A), // Soft Red
            Color(0xFFFFAB91), // Soft Deep Orange
            Color(0xFFFFE082), // Soft Amber
            Color(0xFFCE93D8), // Soft Purple
            Color(0xFF9FA8DA), // Indigo
            Color(0xFFB39DDB), // Lavender
            Color(0xFF9FA8DA), // Soft Blue
            Color(0xFFFFF59D), // Yellow
            Color(0xFFFFCC80), // Soft Orange
            Color(0xFFA1887F), // Brown
            Color(0xFF80CBC4), // Teal
            Color(0xFFA5D6A7), // Green
            Color(0xFFC5E1A5), // Light Green
            Color(0xFFE6EE9C), // Lime
            Color(0xFFCFD8DC), // Blue Grey
            Color(0xFFFFAB91), // Coral
            Color(0xFFFFCC80), // Apricot
            Color(0xFFFFE082), // Yellowish
            Color(0xFF90CAF9), // Blue
            Color(0xFF64B5F6), // Bright Blue
            Color(0xFF4FC3F7), // Sky Blue
            Color(0xFFB0BEC5), // Muted Grey Blue
        )
    }

    override val divider: DividerColors = object : DividerColors {
        override val primary: Color = Neutral300
        override val secondary: Color = Neutral400
    }

    override val themeColors: AppColor.ThemeColors = object : AppColor.ThemeColors {
        override val lightText: Color = Color(0xFF000000)
        override val lightBackground1: Color = Color(0xFFF6F9FF)
        override val lightBackground2: Color = Color(0xFFFFFFFF)
        override val darkText: Color = Color(0xFFFFFFFF)
        override val darkBackground1: Color = Color(0xFF121212)
        override val darkBackground2: Color = Color(0xFF181818)
    }

    override val konfetti: AppColor.Konfetti = object : AppColor.Konfetti {
        override val confettiColor1 = Success
        override val confettiColor2 = Warning
        override val confettiColor3 = Error
        override val confettiColor4 = Primary600
        override val confettiColor5 = Primary500
        override val confettiColor6 = Primary700
        override val confettiColor7 = Primary800
        override val confettiColor8 = Neutral500
        override val confettiColor9 = Neutral600
        override val confettiColor10 = Neutral700
    }

    override val chip: AppColor.ChipColors = object : AppColor.ChipColors {
        override val intensity: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFFFF6B6B) // Brighter red for dark theme
                override val endColor = Color(0xFFFF9F4F)   // Brighter orange for dark theme
                override val contentColor = White
                override val borderColor = Color(0xFFFF6B6B).copy(alpha = 0.5f)
            }

        override val tonnage: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFF4A8FFF) // Brighter blue for dark theme
                override val endColor = Color(0xFF33D2FF)   // Brighter cyan for dark theme
                override val contentColor = White
                override val borderColor = Color(0xFF4A8FFF).copy(alpha = 0.5f)
            }

        override val repetitions: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFFA56FFF) // Brighter purple for dark theme
                override val endColor = Color(0xFFC87AFF)   // Brighter light purple for dark theme
                override val contentColor = White
                override val borderColor = Color(0xFFA56FFF).copy(alpha = 0.5f)
            }
    }
}
