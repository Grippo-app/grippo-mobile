package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.provider.AppColor

public object DarkColor : AppColor {

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = AppColorTokens.NeutralDark.N250
        override val focus = AppColorTokens.Common.Accent
    }

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = AppColorTokens.Primary.P600
        override val textPrimary = AppColorTokens.Common.White
        override val iconPrimary = AppColorTokens.Common.White
        override val backgroundPrimaryDisabled = AppColorTokens.NeutralDark.N300
        override val contentPrimaryDisabled = AppColorTokens.NeutralDark.N550

        override val backgroundSecondary = Color.Transparent
        override val textSecondary = AppColorTokens.Common.White
        override val iconSecondary = AppColorTokens.Common.Accent
        override val borderSecondary = AppColorTokens.Common.Accent
        override val backgroundSecondaryDisabled = AppColorTokens.NeutralDark.N300
        override val contentSecondaryDisabled = AppColorTokens.NeutralDark.N550

        override val textTransparent = AppColorTokens.Common.Accent
        override val iconTransparent = AppColorTokens.Common.Accent
        override val contentTransparentDisabled = AppColorTokens.NeutralDark.N550
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = AppColorTokens.NeutralDark.N700
        override val secondary = AppColorTokens.NeutralDark.N600
        override val disabled = AppColorTokens.NeutralDark.N300
        override val accent = AppColorTokens.Common.Accent
        override val inverted = AppColorTokens.Common.Black
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = AppColorTokens.Common.White
        override val checkedTrack = AppColorTokens.Common.Accent

        override val uncheckedThumb = AppColorTokens.Common.White
        override val uncheckedTrack = AppColorTokens.NeutralDark.N300
    }

    override val radio: AppColor.RadioColors = object : AppColor.RadioColors {
        override val selectedThumb = AppColorTokens.Common.Accent
        override val selectedTrack = AppColorTokens.Common.Accent

        override val unselectedTrack = AppColorTokens.NeutralDark.N300

        override val disabledSelectedThumb = AppColorTokens.NeutralDark.N300
        override val disabledUnselectedTrack = AppColorTokens.NeutralDark.N300
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val default = AppColorTokens.NeutralDark.N200
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val placeholder = AppColorTokens.NeutralDark.N500
        override val label = AppColorTokens.NeutralDark.N500
        override val text = AppColorTokens.NeutralDark.N800

        override val leading = AppColorTokens.NeutralDark.N600
        override val trailing = AppColorTokens.NeutralDark.N600

        override val backgroundDisabled = AppColorTokens.NeutralDark.N300
        override val textDisabled = AppColorTokens.NeutralDark.N500
        override val placeholderDisabled = AppColorTokens.NeutralDark.N500
    }

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val screen = AppColorTokens.NeutralDark.N050
        override val dialog = AppColorTokens.NeutralDark.N100
        override val card = AppColorTokens.NeutralDark.N150
        override val accent = AppColorTokens.Common.Accent
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val handle = AppColorTokens.NeutralDark.N400
        override val scrim = Color(0xB3000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppColorTokens.NeutralDark.N800
        override val secondary = AppColorTokens.NeutralDark.N700
        override val tertiary = AppColorTokens.NeutralDark.N500
        override val inverted = AppColorTokens.Common.Black
        override val disabled = AppColorTokens.NeutralDark.N400
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = AppColorTokens.Semantic.Success
        override val error = AppColorTokens.Semantic.Error
        override val warning = AppColorTokens.Semantic.Warning
        override val info = AppColorTokens.Common.Accent
        override val accent = AppColorTokens.Common.Accent
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow = AppColorTokens.Common.Black.copy(alpha = 0.2f)
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val background = AppColorTokens.NeutralDark.N250
        override val shimmer = AppColorTokens.NeutralDark.N400
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val active = AppColorTokens.NeutralDark.N800
        override val inactive = AppColorTokens.NeutralDark.N500
        override val selector = AppColorTokens.Common.Accent
    }

    override val theme: AppColor.ThemeColors = object : AppColor.ThemeColors {
        override val lightText = AppColorTokens.NeutralLight.N800
        override val lightBackground1 = AppColorTokens.Common.White
        override val lightBackground2 = AppColorTokens.Common.White
        override val darkText = AppColorTokens.NeutralDark.N800
        override val darkBackground1 = AppColorTokens.NeutralDark.N150
        override val darkBackground2 = AppColorTokens.NeutralDark.N150
    }

    override val konfetti: AppColor.Konfetti = object : AppColor.Konfetti {
        override val confettiColor1 = AppColorTokens.Semantic.Success
        override val confettiColor2 = AppColorTokens.Semantic.Warning
        override val confettiColor3 = AppColorTokens.Semantic.Error
        override val confettiColor4 = AppColorTokens.Common.Accent
        override val confettiColor5 = AppColorTokens.Primary.P500
        override val confettiColor6 = AppColorTokens.Primary.P600
        override val confettiColor7 = AppColorTokens.Primary.P700
        override val confettiColor8 = AppColorTokens.NeutralDark.N500
        override val confettiColor9 = AppColorTokens.NeutralDark.N600
        override val confettiColor10 = AppColorTokens.NeutralDark.N700
    }

    override val chip: AppColor.ChipColors = object : AppColor.ChipColors {
        override val intensity: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFFFF6B6B)
                override val endColor = Color(0xFFFF9F4F)
                override val contentColor = AppColorTokens.Common.White
            }

        override val volume: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFF4A8FFF)
                override val endColor = Color(0xFF33D2FF)
                override val contentColor = AppColorTokens.Common.White
            }

        override val repetitions: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFFA56FFF)
                override val endColor = Color(0xFFC87AFF)
                override val contentColor = AppColorTokens.Common.White
            }
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val focused = AppColorTokens.Primary.P600
        override val active = AppColorTokens.Primary.P500
        override val inactive = AppColorTokens.NeutralDark.N400
        override val background = AppColorTokens.NeutralDark.N300
        override val outline = AppColorTokens.NeutralDark.N400
        override val text = AppColorTokens.Common.White
        override val palette = listOf(
            Color(0xFFEF9A9A), Color(0xFFFFAB91), Color(0xFFFFE082), Color(0xFFCE93D8),
            Color(0xFF9FA8DA), Color(0xFFB39DDB), Color(0xFF9FA8DA), Color(0xFFFFF59D),
            Color(0xFFFFCC80), Color(0xFFA1887F), Color(0xFF80CBC4), Color(0xFFA5D6A7),
            Color(0xFFC5E1A5), Color(0xFFE6EE9C), Color(0xFFCFD8DC), Color(0xFFFFAB91),
            Color(0xFFFFCC80), Color(0xFFFFE082), Color(0xFF90CAF9), Color(0xFF64B5F6),
            Color(0xFF4FC3F7), Color(0xFFB0BEC5)
        )
    }
}