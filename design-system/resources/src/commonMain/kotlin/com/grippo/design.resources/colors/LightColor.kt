package com.grippo.design.resources.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.AppColor

public object LightColor : AppColor {
    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val defaultPrimary = AppColorTokens.NeutralLight.N200
        override val disabledPrimary = AppColorTokens.NeutralLight.N200
        override val defaultSecondary = AppColorTokens.Primary.P150
        override val disabledSecondary = AppColorTokens.NeutralLight.N200
        override val inverted = AppColorTokens.Common.White
        override val focus = AppColorTokens.Common.Accent
        override val error = AppColorTokens.Semantic.Error
    }

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = AppColorTokens.Primary.P500
        override val textPrimary = AppColorTokens.Common.White
        override val iconPrimary = AppColorTokens.Common.White
        override val backgroundPrimaryDisabled = AppColorTokens.NeutralLight.N200
        override val contentPrimaryDisabled = AppColorTokens.NeutralLight.N500

        override val backgroundSecondary = Color.Transparent
        override val textSecondary = AppColorTokens.Common.Black
        override val iconSecondary = AppColorTokens.Common.Accent
        override val borderSecondary = AppColorTokens.Primary.P250
        override val backgroundSecondaryDisabled = AppColorTokens.NeutralLight.N200
        override val contentSecondaryDisabled = AppColorTokens.NeutralLight.N500

        override val textTransparent = AppColorTokens.Common.Accent
        override val iconTransparent = AppColorTokens.Common.Accent
        override val contentTransparentDisabled = AppColorTokens.NeutralLight.N500
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = AppColorTokens.NeutralLight.N700
        override val secondary = AppColorTokens.NeutralLight.N600
        override val accent = AppColorTokens.Common.Accent
        override val inverted = AppColorTokens.Common.White
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = AppColorTokens.Common.White
        override val checkedTrack = AppColorTokens.Common.Accent

        override val uncheckedThumb = AppColorTokens.Common.White
        override val uncheckedTrack = AppColorTokens.NeutralLight.N200
    }

    override val radio: AppColor.RadioColors = object : AppColor.RadioColors {
        override val selectedThumb = AppColorTokens.Common.Accent
        override val selectedTrack = AppColorTokens.Common.Accent

        override val unselectedTrack = AppColorTokens.NeutralLight.N200

        override val disabledSelectedThumb = AppColorTokens.NeutralLight.N500
        override val disabledUnselectedTrack = AppColorTokens.NeutralLight.N200
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val background = AppColorTokens.Common.White
        override val border = AppColorTokens.NeutralLight.N200
        override val placeholder = AppColorTokens.NeutralLight.N400
        override val label = AppColorTokens.NeutralLight.N500
        override val text = AppColorTokens.Common.Black

        override val leading = AppColorTokens.NeutralLight.N700
        override val trailing = AppColorTokens.NeutralLight.N700

        override val backgroundDisabled = AppColorTokens.NeutralLight.N200
        override val borderDisabled = AppColorTokens.NeutralLight.N200
        override val textDisabled = AppColorTokens.NeutralLight.N400
        override val placeholderDisabled = AppColorTokens.NeutralLight.N400
    }

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val primary = AppColorTokens.NeutralLight.N150
        override val secondary = AppColorTokens.Common.White
        override val tertiary = AppColorTokens.NeutralLight.N100
        override val disabled = AppColorTokens.NeutralLight.N200
        override val accent = AppColorTokens.Common.Accent
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val background = AppColorTokens.Common.White
        override val handle = AppColorTokens.NeutralLight.N500
        override val scrim = Color(0x80000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppColorTokens.Common.Black
        override val secondary = AppColorTokens.NeutralLight.N700
        override val tertiary = AppColorTokens.NeutralLight.N400
        override val inverted = AppColorTokens.Common.White
        override val disabled = AppColorTokens.NeutralLight.N300
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = AppColorTokens.Semantic.Success
        override val error = AppColorTokens.Semantic.Error
        override val warning = AppColorTokens.Semantic.Warning
        override val info = AppColorTokens.Common.Accent
        override val accent = AppColorTokens.Common.Accent
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow = Color(0x26000000)
        override val accentShadow = AppColorTokens.Common.Accent.copy(alpha = 0.35f)
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val background = AppColorTokens.NeutralLight.N300
        override val shimmer = AppColorTokens.NeutralLight.N400
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val active = AppColorTokens.Common.Black
        override val inactive = AppColorTokens.NeutralLight.N500
        override val selector = AppColorTokens.Common.Accent
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val primary = AppColorTokens.NeutralLight.N250
        override val secondary = AppColorTokens.NeutralLight.N200
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
        override val confettiColor5 = AppColorTokens.Primary.P300
        override val confettiColor6 = AppColorTokens.Primary.P400
        override val confettiColor7 = AppColorTokens.Primary.P500
        override val confettiColor8 = AppColorTokens.NeutralLight.N300
        override val confettiColor9 = AppColorTokens.NeutralLight.N400
        override val confettiColor10 = AppColorTokens.NeutralLight.N500
    }

    override val chip: AppColor.ChipColors = object : AppColor.ChipColors {
        override val intensity: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFFFF5757)
                override val endColor = Color(0xFFFF8C29)
                override val contentColor = AppColorTokens.Common.White
                override val borderColor = startColor.copy(alpha = 0.5f)
            }

        override val tonnage: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFF2B7FFF)
                override val endColor = Color(0xFF00C2FF)
                override val contentColor = AppColorTokens.Common.White
                override val borderColor = startColor.copy(alpha = 0.5f)
            }

        override val repetitions: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFF8C52FF)
                override val endColor = Color(0xFFAF5CF7)
                override val contentColor = AppColorTokens.Common.White
                override val borderColor = startColor.copy(alpha = 0.5f)
            }
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val focused = AppColorTokens.Primary.P600
        override val active = AppColorTokens.Primary.P400
        override val inactive = AppColorTokens.NeutralLight.N300
        override val background = AppColorTokens.NeutralLight.N200
        override val outline = AppColorTokens.NeutralLight.N200
        override val palette = listOf(
            Color(0xFFEF5350), Color(0xFFFF7043), Color(0xFFFFCA28), Color(0xFFBA68C8),
            Color(0xFF5C6BC0), Color(0xFF9575CD), Color(0xFF7986CB), Color(0xFFFFEE58),
            Color(0xFFFFA726), Color(0xFF8D6E63), Color(0xFF4DB6AC), Color(0xFF81C784),
            Color(0xFFAED581), Color(0xFFDCE775), Color(0xFFB0BEC5), Color(0xFFFF8A65),
            Color(0xFFFFB74D), Color(0xFFFFD54F), Color(0xFF64B5F6), Color(0xFF42A5F5),
            Color(0xFF4FC3F7), Color(0xFF90A4AE)
        )
    }
}
