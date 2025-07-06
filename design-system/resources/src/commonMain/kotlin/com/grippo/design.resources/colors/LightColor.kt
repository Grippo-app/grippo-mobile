package com.grippo.design.resources.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.AppColor
import com.grippo.design.resources.AppColor.DividerColors

public object LightColor : AppColor {
    private val Transparent = Color(0x00000000)
    private val White = Color(0xFFFFFFFF)
    private val Black = Color(0xFF000000)
    private val Error = Color(0xFFFF4D4F)
    private val Warning = Color(0xFFFFAA2C)
    private val Success = Color(0xFF3AC86B)

    private val Primary100 = Color(0xFFE6ECFF)
    private val Primary200 = Color(0xFFC2D1FF)
    private val Primary300 = Color(0xFF99B2FF)
    private val Primary400 = Color(0xFF7090FF)
    private val Primary500 = Color(0xFF3366FF)
    private val Primary600 = Color(0xFF2B57E6)
    private val Primary700 = Color(0xFF244ACC)
    private val Primary800 = Color(0xFF1C3DAF)
    private val Primary900 = Color(0xFF152F94)

    private val Neutral100 = Color(0xFFFAFAFA)
    private val Neutral200 = Color(0xFFF2F2F2)
    private val Neutral300 = Color(0xFFDCDCDC)
    private val Neutral400 = Color(0xFFBFBFBF)
    private val Neutral500 = Color(0xFF999999)
    private val Neutral600 = Color(0xFF666666)
    private val Neutral700 = Color(0xFF333333)

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = Primary500
        override val contentPrimary = White
        override val backgroundPrimaryDisabled = Neutral200
        override val contentPrimaryDisabled = Neutral500

        override val backgroundSecondary = Transparent
        override val contentSecondary = Primary500
        override val backgroundSecondaryDisabled = Neutral100
        override val contentSecondaryDisabled = Neutral300

        override val contentTransparentDisabled = Neutral300
    }

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val defaultPrimary = Neutral200
        override val disabledPrimary = Neutral200
        override val defaultSecondary = Primary200
        override val disabledSecondary = Neutral200

        override val focus = Primary500
        override val error = Error
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val default = Neutral700
        override val disabled = Neutral300
        override val accent = Primary500
        override val invert = White
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
        override val primary = Neutral100
        override val secondary = White
        override val accent = Primary500
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val background = White
        override val handle = Neutral300
        override val scrim = Color(0x80000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = Black
        override val inverted = White
        override val secondary = Neutral600
        override val disabled = Neutral300
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = Success
        override val error = Error
        override val warning = Warning
        override val info = Primary500
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow: Color = Color(0x26000000)
        override val accentShadow: Color = Primary500.copy(alpha = 0.35f)
    }

    override val interaction: AppColor.InteractionColors = object : AppColor.InteractionColors {
        override val pressed = Neutral200
        override val hovered = Neutral100
        override val focused = Primary500
    }

    override val elevation: AppColor.ElevationColors = object : AppColor.ElevationColors {
        override val level0 = Color(0x00000000)
        override val level1 = Color(0x05000000)
        override val level2 = Color(0x0A000000)
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
        override val default: Color = Neutral200
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
}