package com.grippo.design.resources.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.AppColor
import com.grippo.design.resources.AppColor.DividerColors

public object LightColor : AppColor {
    private val White = Color(0xFFFFFFFF)
    private val Black = Color(0xFF000000)
    private val Error = Color(0xFFFF4D4F)
    private val Warning = Color(0xFFFFAA2C)
    private val Success = Color(0xFF3AC86B)

    private val Primary100 = Color(0xFFE6ECFF)
    private val Primary150 = Color(0xFFD4DFFF)
    private val Primary200 = Color(0xFFC2D1FF)
    private val Primary300 = Color(0xFF99B2FF)
    private val Primary400 = Color(0xFF7090FF)
    private val Primary500 = Color(0xFF3366FF)
    private val Primary600 = Color(0xFF2B57E6)
    private val Primary700 = Color(0xFF244ACC)
    private val Primary800 = Color(0xFF1C3DAF)
    private val Primary900 = Color(0xFF152F94)

    private val Neutral100 = Color(0xFFF9FBFE)
    private val Neutral150 = Color(0xFFF6F9FF)
    private val Neutral200 = Color(0xFFE9EFF8)
    private val Neutral250 = Color(0xFFE1EAF6)
    private val Neutral300 = Color(0xFFC5D2E6)
    private val Neutral400 = Color(0xFFA7B8D1)
    private val Neutral500 = Color(0xFF8294B3)
    private val Neutral600 = Color(0xFF5F6C88)
    private val Neutral700 = Color(0xFF3E4961)

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = Primary500
        override val textPrimary = White
        override val iconPrimary = White
        override val backgroundPrimaryDisabled = Neutral200
        override val contentPrimaryDisabled = Neutral500

        override val backgroundSecondary = Neutral200
        override val textSecondary = Black
        override val iconSecondary = Primary500
        override val borderSecondary = Primary150
        override val backgroundSecondaryDisabled = Neutral200
        override val contentSecondaryDisabled = Neutral500

        override val textTransparent: Color = Primary500
        override val iconTransparent: Color = Primary500
        override val contentTransparentDisabled = Neutral500
    }

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val defaultPrimary = Neutral200
        override val disabledPrimary = Neutral200
        override val defaultSecondary = Primary150
        override val disabledSecondary = Neutral200
        override val inverted = White

        override val focus = Primary500
        override val error = Error
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = Neutral700
        override val secondary = Neutral500
        override val disabled = Neutral300
        override val accent = Primary500
        override val inverted = White
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = White
        override val checkedTrack = Primary500

        override val uncheckedThumb = White
        override val uncheckedTrack = Neutral200

        override val disabledCheckedThumb = Neutral500
        override val disabledCheckedTrack = Neutral200

        override val disabledUncheckedThumb = Neutral300
        override val disabledUncheckedTrack = Neutral200
        override val disabledUncheckedBorder = Neutral200
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val background = White
        override val border = Neutral400
        override val placeholder = Neutral400
        override val label = Neutral500
        override val text = Black

        override val leading: Color = Neutral700
        override val trailing: Color = Neutral700

        override val backgroundDisabled = Neutral200
        override val borderDisabled = Neutral400
        override val textDisabled = Neutral400
        override val placeholderDisabled = Neutral400
    }

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val primary = Neutral150
        override val secondary = White
        override val accent = Primary500
        override val tertiary = Neutral250
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val background = White
        override val handle = Neutral500
        override val scrim = Color(0x80000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = Black
        override val secondary = Neutral700
        override val tertiary = Neutral400
        override val inverted = White
        override val disabled = Neutral300
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = Success
        override val error = Error
        override val warning = Warning
        override val info = Primary500
        override val accent = Primary500
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow: Color = Color(0x26000000)
        override val accentShadow: Color = Primary500.copy(alpha = 0.35f)
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val background = Neutral300
        override val shimmer = Neutral400
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val active = Black
        override val inactive: Color = Neutral500
        override val selector: Color = Primary500
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val focused = Primary600
        override val active = Primary400
        override val inactive = Neutral300
        override val background = Neutral200
        override val outline = Neutral200
    }

    override val divider: DividerColors = object : DividerColors {
        override val primary: Color = Neutral200
        override val secondary: Color = Neutral250
    }

    override val konfetti: AppColor.Konfetti = object : AppColor.Konfetti {
        override val confettiColor1 = Success
        override val confettiColor2 = Warning
        override val confettiColor3 = Error
        override val confettiColor4 = Primary500
        override val confettiColor5 = Primary300
        override val confettiColor6 = Primary400
        override val confettiColor7 = Primary500
        override val confettiColor8 = Neutral300
        override val confettiColor9 = Neutral400
        override val confettiColor10 = Neutral500
    }

    override val chip: AppColor.ChipColors = object : AppColor.ChipColors {
        override val intensity: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFFFF5757) // Bright red
                override val endColor = Color(0xFFFF8C29)   // Orange
                override val contentColor = White
                override val borderColor = Color(0xFFFF5757).copy(alpha = 0.5f)
            }

        override val tonnage: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFF2B7FFF) // Blue
                override val endColor = Color(0xFF00C2FF)   // Cyan
                override val contentColor = White
                override val borderColor = Color(0xFF2B7FFF).copy(alpha = 0.5f)
            }

        override val repetitions: AppColor.ChipColors.GradientColors =
            object : AppColor.ChipColors.GradientColors {
                override val startColor = Color(0xFF8C52FF) // Purple
                override val endColor = Color(0xFFAF5CF7)   // Light purple
                override val contentColor = White
                override val borderColor = Color(0xFF8C52FF).copy(alpha = 0.5f)
            }
    }
}
