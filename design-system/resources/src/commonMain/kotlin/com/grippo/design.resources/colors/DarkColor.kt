package com.grippo.design.resources.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.AppColor

public object DarkColor : AppColor {

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val defaultPrimary = AppColorTokens.NeutralDark.N200
        override val disabledPrimary = Color(0xFF2A2A2A)
        override val defaultSecondary = AppColorTokens.Primary.P250
        override val disabledSecondary = Color(0xFF2A2A2A)
        override val inverted = AppColorTokens.NeutralDark.N100
        override val focus = AppColorTokens.Common.Accent
        override val error = AppColorTokens.Semantic.Error
    }

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = AppColorTokens.Primary.P600
        override val textPrimary = AppColorTokens.Common.White
        override val iconPrimary = AppColorTokens.Common.White
        override val backgroundPrimaryDisabled = Color(0xFF303030)
        override val contentPrimaryDisabled = Color(0xFFA0A0A0)

        override val backgroundSecondary = Color.Transparent
        override val textSecondary = AppColorTokens.Common.White
        override val iconSecondary = AppColorTokens.Common.Accent
        override val borderSecondary = AppColorTokens.Common.Accent
        override val backgroundSecondaryDisabled = Color(0xFF303030)
        override val contentSecondaryDisabled = AppColorTokens.NeutralDark.N500

        override val textTransparent = AppColorTokens.Common.Accent
        override val iconTransparent = AppColorTokens.Common.Accent
        override val contentTransparentDisabled = AppColorTokens.NeutralDark.N500
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = AppColorTokens.NeutralDark.N700
        override val secondary = AppColorTokens.NeutralDark.N600
        override val disabled = AppColorTokens.NeutralDark.N400
        override val accent = AppColorTokens.Common.Accent
        override val inverted = AppColorTokens.Common.Black
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = AppColorTokens.Common.White
        override val checkedTrack = AppColorTokens.Common.Accent

        override val uncheckedThumb = AppColorTokens.Common.White
        override val uncheckedTrack = Color(0xFF3A3A3A)

        override val disabledCheckedThumb = Color(0xFFA0A0A0)
        override val disabledCheckedTrack = Color(0xFF2A2A2A)
        override val disabledUncheckedThumb = Color(0xFF4D4D4D)
        override val disabledUncheckedTrack = Color(0xFF2A2A2A)
        override val disabledUncheckedBorder = Color(0xFF3A3A3A)
    }

    override val radio: AppColor.RadioColors = object : AppColor.RadioColors {
        override val selectedThumb = AppColorTokens.Common.Accent
        override val selectedTrack = AppColorTokens.Common.Accent

        override val unselectedTrack = Color(0xFF3A3A3A)
        override val unselectedThumb = AppColorTokens.Common.White

        override val disabledSelectedThumb = Color(0xFF2A2A2A)
        override val disabledSelectedTrack = Color(0xFF2A2A2A)
        override val disabledUnselectedThumb = Color(0xFF2A2A2A)
        override val disabledUnselectedTrack = Color(0xFF2A2A2A)
        override val disabledUnselectedBorder = Color(0xFF2A2A2A)
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val primary = AppColorTokens.NeutralDark.N250
        override val secondary = AppColorTokens.NeutralDark.N200
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val background = AppColorTokens.NeutralDark.N200
        override val border = AppColorTokens.NeutralDark.N400
        override val placeholder = AppColorTokens.NeutralDark.N450
        override val label = AppColorTokens.NeutralDark.N550
        override val text = AppColorTokens.Common.White

        override val leading = AppColorTokens.NeutralDark.N700
        override val trailing = AppColorTokens.NeutralDark.N700

        override val backgroundDisabled = AppColorTokens.NeutralDark.N300
        override val borderDisabled = AppColorTokens.NeutralDark.N350
        override val textDisabled = AppColorTokens.NeutralDark.N450
        override val placeholderDisabled = AppColorTokens.NeutralDark.N350
    }

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val primary = AppColorTokens.NeutralDark.N100
        override val secondary = AppColorTokens.NeutralDark.N150
        override val accent = AppColorTokens.Common.Accent
        override val tertiary = AppColorTokens.NeutralDark.N200
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val background = AppColorTokens.NeutralDark.N150
        override val handle = AppColorTokens.NeutralDark.N600
        override val scrim = Color(0xB3000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppColorTokens.Common.White
        override val secondary = AppColorTokens.NeutralDark.N700
        override val tertiary = AppColorTokens.NeutralDark.N600
        override val inverted = AppColorTokens.Common.Black
        override val disabled = AppColorTokens.NeutralDark.N500
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = AppColorTokens.Semantic.Success
        override val error = AppColorTokens.Semantic.Error
        override val warning = AppColorTokens.Semantic.Warning
        override val info = AppColorTokens.Common.Accent
        override val accent = AppColorTokens.Common.Accent
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow = Color(0x40000000)
        override val accentShadow = AppColorTokens.Common.Accent.copy(alpha = 0.35f)
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val background = AppColorTokens.NeutralDark.N250
        override val shimmer = AppColorTokens.NeutralDark.N400
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val active = AppColorTokens.Common.White
        override val inactive = AppColorTokens.NeutralDark.N500
        override val selector = AppColorTokens.Common.Accent
    }

    override val theme: AppColor.ThemeColors = object : AppColor.ThemeColors {
        override val lightText = AppColorTokens.Common.Black
        override val lightBackground1 = AppColorTokens.NeutralLight.N150
        override val lightBackground2 = AppColorTokens.Common.White
        override val darkText = AppColorTokens.Common.White
        override val darkBackground1 = AppColorTokens.NeutralDark.N100
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
                override val borderColor = startColor.copy(alpha = 0.5f)
            }

        override val tonnage: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFF4A8FFF)
                override val endColor = Color(0xFF33D2FF)
                override val contentColor = AppColorTokens.Common.White
                override val borderColor = startColor.copy(alpha = 0.5f)
            }

        override val repetitions: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFFA56FFF)
                override val endColor = Color(0xFFC87AFF)
                override val contentColor = AppColorTokens.Common.White
                override val borderColor = startColor.copy(alpha = 0.5f)
            }
    }

    override val equipment: AppColor.EquipmentColors = object : AppColor.EquipmentColors {
        override val background: Color = AppColorTokens.Primary.P900
        override val border: Color = AppColorTokens.Primary.P300
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val focused = AppColorTokens.Primary.P600
        override val active = AppColorTokens.Primary.P500
        override val inactive = AppColorTokens.NeutralDark.N400
        override val background = AppColorTokens.NeutralDark.N300
        override val outline = AppColorTokens.NeutralDark.N400
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