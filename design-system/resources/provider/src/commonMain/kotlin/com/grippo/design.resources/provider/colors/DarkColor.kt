package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.provider.AppColor

public object DarkColor : AppColor {

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = AppPalette.NeutralDark.N250
        override val focus = AppPalette.Common.Accent
    }

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary = AppPalette.Primary.P600
        override val textPrimary = AppPalette.Common.White
        override val iconPrimary = AppPalette.Common.White
        override val backgroundPrimaryDisabled = AppPalette.NeutralDark.N300
        override val contentPrimaryDisabled = AppPalette.NeutralDark.N550

        override val backgroundSecondary = Color.Transparent
        override val textSecondary = AppPalette.Common.White
        override val iconSecondary = AppPalette.Common.Accent
        override val borderSecondary = AppPalette.Common.Accent
        override val backgroundSecondaryDisabled = AppPalette.NeutralDark.N300
        override val contentSecondaryDisabled = AppPalette.NeutralDark.N550

        override val textTransparent = AppPalette.Common.Accent
        override val iconTransparent = AppPalette.Common.Accent
        override val contentTransparentDisabled = AppPalette.NeutralDark.N550
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = AppPalette.NeutralDark.N700
        override val secondary = AppPalette.NeutralDark.N600
        override val disabled = AppPalette.NeutralDark.N300
        override val accent = AppPalette.Common.Accent
        override val inverted = AppPalette.Common.Black
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = AppPalette.Common.White
        override val checkedTrack = AppPalette.Common.Accent
        override val uncheckedThumb = AppPalette.Common.White
        override val uncheckedTrack = AppPalette.NeutralDark.N300
    }

    override val radio: AppColor.RadioColors = object : AppColor.RadioColors {
        override val selectedThumb = AppPalette.Common.Accent
        override val selectedTrack = AppPalette.Common.Accent
        override val unselectedTrack = AppPalette.NeutralDark.N300
        override val disabledSelectedThumb = AppPalette.NeutralDark.N300
        override val disabledUnselectedTrack = AppPalette.NeutralDark.N300
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val default = AppPalette.NeutralDark.N200
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val placeholder = AppPalette.NeutralDark.N500
        override val label = AppPalette.NeutralDark.N500
        override val text = AppPalette.NeutralDark.N800
        override val leading = AppPalette.NeutralDark.N600
        override val trailing = AppPalette.NeutralDark.N600
        override val backgroundDisabled = AppPalette.NeutralDark.N300
        override val textDisabled = AppPalette.NeutralDark.N500
        override val placeholderDisabled = AppPalette.NeutralDark.N500
    }

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val screen = AppPalette.NeutralDark.N050
        override val dialog = AppPalette.NeutralDark.N100
        override val card = AppPalette.NeutralDark.N150
        override val accent = AppPalette.Common.Accent
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val handle = AppPalette.NeutralDark.N400
        override val scrim = Color(0xB3000000)
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppPalette.NeutralDark.N800
        override val secondary = AppPalette.NeutralDark.N700
        override val tertiary = AppPalette.NeutralDark.N500
        override val inverted = AppPalette.Common.Black
        override val disabled = AppPalette.NeutralDark.N400
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = AppPalette.Semantic.Success
        override val error = AppPalette.Semantic.Error
        override val warning = AppPalette.Semantic.Warning
        override val info = AppPalette.Common.Accent
        override val accent = AppPalette.Common.Accent
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow = AppPalette.Common.Black.copy(alpha = 0.2f)
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val background = AppPalette.NeutralDark.N250
        override val shimmer = AppPalette.NeutralDark.N400
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val active = AppPalette.NeutralDark.N800
        override val inactive = AppPalette.NeutralDark.N500
        override val selector = AppPalette.Common.Accent
    }

    override val theme: AppColor.ThemeColors = object : AppColor.ThemeColors {
        override val lightText = AppPalette.NeutralLight.N800
        override val lightBackground1 = AppPalette.Common.White
        override val lightBackground2 = AppPalette.Common.White
        override val darkText = AppPalette.NeutralDark.N800
        override val darkBackground1 = AppPalette.NeutralDark.N150
        override val darkBackground2 = AppPalette.NeutralDark.N150
    }

    override val konfetti: AppColor.Konfetti = object : AppColor.Konfetti {
        override val confettiColor1 = AppPalette.Semantic.Success
        override val confettiColor2 = AppPalette.Semantic.Warning
        override val confettiColor3 = AppPalette.Semantic.Error
        override val confettiColor4 = AppPalette.Common.Accent
        override val confettiColor5 = AppPalette.Primary.P500
        override val confettiColor6 = AppPalette.Primary.P600
        override val confettiColor7 = AppPalette.Primary.P700
        override val confettiColor8 = AppPalette.NeutralDark.N500
        override val confettiColor9 = AppPalette.NeutralDark.N600
        override val confettiColor10 = AppPalette.NeutralDark.N700
    }

    override val chip: AppColor.ChipColors = object : AppColor.ChipColors {
        override val intensity = object : AppColor.ChipColors.GradientColors {
            override val startColor = Color(0xFFFF6B6B)
            override val endColor = Color(0xFFFF9F4F)
            override val contentColor = AppPalette.Common.White
        }
        override val volume = object : AppColor.ChipColors.GradientColors {
            override val startColor = Color(0xFF4A8FFF)
            override val endColor = Color(0xFF33D2FF)
            override val contentColor = AppPalette.Common.White
        }
        override val repetitions = object : AppColor.ChipColors.GradientColors {
            override val startColor = Color(0xFFA56FFF)
            override val endColor = Color(0xFFC87AFF)
            override val contentColor = AppPalette.Common.White
        }
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val focused = AppPalette.Primary.P600
        override val active = AppPalette.Primary.P500
        override val inactive = AppPalette.NeutralDark.N400
        override val background = AppPalette.NeutralDark.N300
        override val outline = AppPalette.NeutralDark.N400
        override val text = AppPalette.Common.White
        override val palette = AppPalette.Qualitative.Palette12
        override val scaleStops = AppPalette.Ramps.BlueOrangeRed
    }

    override val charts: AppColor.Charts = object : AppColor.Charts {
        override val surface = object : AppColor.Charts.SurfaceColors {
            override val grid = AppPalette.Common.White.copy(alpha = 0.13f)
            override val axis = AppPalette.Common.White.copy(alpha = 0.20f)
            override val labelPrimary = AppPalette.NeutralDark.N700
            override val labelSecondary = AppPalette.NeutralDark.N500
            override val extremaLabel = AppPalette.Common.White.copy(alpha = 0.80f)
        }
        override val sparkline = object : AppColor.Charts.SparklineColors {
            override val lineA = AppPalette.Primary.P400
            override val lineB = AppPalette.Semantic.Success
            override val fillBase = AppPalette.Primary.P400
            override val dot = AppPalette.Primary.P400
            override val min = AppPalette.Semantic.Warning
            override val max = AppPalette.Semantic.Success
        }
        override val area = object : AppColor.Charts.AreaColors {
            override val lineA = AppPalette.Semantic.Success
            override val lineB = AppPalette.Primary.P400
            override val fillBase = AppPalette.Semantic.Success
            override val glow = AppPalette.Semantic.Success
            override val dot = AppPalette.Semantic.Success
        }
        override val bar = object : AppColor.Charts.BarColors {
            override val stroke = AppPalette.Common.White.copy(alpha = 0.20f)
        }
        override val categorical = object : AppColor.Charts.CategoricalColors {
            override val palette = AppPalette.Qualitative.Palette12
        }
        override val pie = object : AppColor.Charts.PieColors {
            override val labelText = AppPalette.Common.White.copy(alpha = 0.80f)
            override val leader = null
        }
        override val heatmap = object : AppColor.Charts.HeatmapColors {
            override val scaleStops = AppPalette.Ramps.BlueOrangeRed
            override val missingCell = AppPalette.Common.White.copy(alpha = 0.07f)
            override val border = AppPalette.Common.White.copy(alpha = 0.20f)
            override val valueText = AppPalette.Common.White
        }
        override val radar = object : AppColor.Charts.RadarColors {
            override val grid = AppPalette.Common.White.copy(alpha = 0.20f)
            override val spoke = AppPalette.Common.White.copy(alpha = 0.13f)
            override val label = AppPalette.NeutralDark.N700
            override val valueText = AppPalette.Common.White.copy(alpha = 0.80f)
            override val strokeFallback = AppPalette.Primary.P500
            override val palette = AppPalette.Qualitative.Palette12
        }
        override val progress = object : AppColor.Charts.ProgressColors {
            override val track = AppPalette.Common.White.copy(alpha = 0.08f)
            override val stroke = AppPalette.Common.White.copy(alpha = 0.20f)
            override val target = AppPalette.Common.White.copy(alpha = 0.27f)
            override val palette = AppPalette.Qualitative.Palette12
        }
    }
}