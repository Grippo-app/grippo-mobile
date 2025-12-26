package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.provider.AppColor

public object DarkColor : AppColor {

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = AppPalette.NeutralDark.N250
        override val focus = AppPalette.NeutralDark.N500
    }

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary1 = AppPalette.Unique.Orange
        override val backgroundPrimary2 = AppPalette.Unique.Red
        override val borderPrimary = AppPalette.Unique.Coral
        override val textPrimary = AppPalette.Common.White
        override val iconPrimary = AppPalette.Common.White

        override val backgroundSecondary1 = AppPalette.Common.White
        override val backgroundSecondary2 = AppPalette.Common.White
        override val borderSecondary = AppPalette.Common.White
        override val textSecondary = AppPalette.Common.Black
        override val iconSecondary = AppPalette.Common.Black

        override val textTertiary: Color = AppPalette.NeutralDark.N800
        override val borderTertiary: Color = Color.Transparent
        override val iconTertiary: Color = AppPalette.NeutralDark.N600

        override val textTransparent = AppPalette.Common.White
        override val iconTransparent = AppPalette.Common.White

        override val backgroundDisabled = AppPalette.NeutralDark.N300
        override val contentDisabled = AppPalette.NeutralDark.N550
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = AppPalette.Common.White
        override val secondary = AppPalette.NeutralDark.N600
        override val tertiary = AppPalette.NeutralDark.N500
        override val disabled = AppPalette.NeutralDark.N300
        override val inverted = AppPalette.Common.Black
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = AppPalette.Common.White
        override val checkedTrack1 = AppPalette.Unique.Green
        override val checkedTrack2 = AppPalette.Unique.Green
        override val uncheckedThumb = AppPalette.Common.White
        override val uncheckedTrack1 = AppPalette.NeutralDark.N200
        override val uncheckedTrack2 = AppPalette.NeutralDark.N300
    }

    override val radio: AppColor.RadioColors = object : AppColor.RadioColors {
        override val selectedThumb = AppPalette.Blue.P500
        override val selectedTrack = AppPalette.Blue.P500
        override val unselectedTrack = AppPalette.NeutralDark.N300
        override val disabledSelectedThumb = AppPalette.NeutralDark.N300
        override val disabledUnselectedTrack = AppPalette.NeutralDark.N300
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val default = AppPalette.NeutralDark.N250
    }

    override val achievements: AppColor.Achievements = object : AppColor.Achievements {
        override val bestTonnage1: Color = AppPalette.Unique.Red
        override val bestTonnage2: Color = AppPalette.Unique.Coral

        override val bestWeight1: Color = AppPalette.Unique.Green
        override val bestWeight2: Color = AppPalette.Unique.Teal

        override val lifetimeVolume1: Color = AppPalette.Unique.Indigo
        override val lifetimeVolume2: Color = AppPalette.Unique.Sky

        override val maxRepetitions1: Color = AppPalette.Unique.Purple
        override val maxRepetitions2: Color = AppPalette.Unique.Violet

        override val peakIntensity1: Color = AppPalette.Unique.Brown
        override val peakIntensity2: Color = AppPalette.Unique.Copper
    }

    override val lineIndicator: AppColor.LineIndicatorColors =
        object : AppColor.LineIndicatorColors {
            override val primary = progressIndicatorColors(AppPalette.Common.White)
            override val success = progressIndicatorColors(AppPalette.Unique.Green)
            override val info = progressIndicatorColors(AppPalette.Common.White)
            override val warning = progressIndicatorColors(AppPalette.Unique.Orange)
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
        override val screen = AppPalette.NeutralDark.N100
        override val dialog = AppPalette.NeutralDark.N150
        override val card = AppPalette.NeutralDark.N250.copy(0.45f)
    }

    override val brand: AppColor.BrandColors = object : AppColor.BrandColors {
        override val color1: Color = AppPalette.Unique.Magenta
        override val color2: Color = AppPalette.Unique.Coral

        override val color3: Color = AppPalette.Unique.Green
        override val color4: Color = AppPalette.Unique.Teal

        override val color5: Color = AppPalette.Unique.Sky
        override val color6: Color = AppPalette.Unique.Indigo
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val handle = AppPalette.NeutralDark.N400
        override val scrim = Color(0xB3000000)
    }

    override val static: AppColor.Static = object : AppColor.Static {
        override val white: Color = AppPalette.Common.White
        override val black: Color = AppPalette.Common.Black
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppPalette.NeutralDark.N800
        override val secondary = AppPalette.NeutralDark.N700
        override val tertiary = AppPalette.NeutralDark.N500
        override val inverted = AppPalette.Common.Black
        override val disabled = AppPalette.NeutralDark.N400
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = AppPalette.Unique.Green
        override val error = AppPalette.Unique.Red
        override val warning = AppPalette.Unique.Orange
        override val info = AppPalette.Common.White
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow = AppPalette.Common.Black.copy(alpha = 0.2f)
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val active = AppPalette.NeutralDark.N800
        override val inactive = AppPalette.NeutralDark.N500
        override val selector = AppPalette.Common.White
    }

    override val konfetti: AppColor.Konfetti = object : AppColor.Konfetti {
        override val confettiColor1 = AppPalette.Unique.Green
        override val confettiColor2 = AppPalette.Unique.Orange
        override val confettiColor3 = AppPalette.Unique.Red
        override val confettiColor4 = AppPalette.Blue.P400
        override val confettiColor5 = AppPalette.Blue.P500
        override val confettiColor6 = AppPalette.Blue.P600
        override val confettiColor7 = AppPalette.Blue.P700
        override val confettiColor8 = AppPalette.NeutralDark.N500
        override val confettiColor9 = AppPalette.NeutralDark.N600
        override val confettiColor10 = AppPalette.NeutralDark.N700
    }

    override val selectableCardColors: AppColor.SelectableCardColors =
        object : AppColor.SelectableCardColors {
            override val small: AppColor.SelectableCardColors.Small =
                object : AppColor.SelectableCardColors.Small {
                    override val selectedBackground1: Color = AppPalette.Blue.P600
                    override val selectedBackground2: Color = AppPalette.Blue.P500
                }
        }

    override val chip: AppColor.ChipColors = object : AppColor.ChipColors {
        override val intensity = object : AppColor.ChipColors.GradientColors {
            override val startColor = AppPalette.Unique.Green
            override val endColor = AppPalette.Unique.Teal
            override val contentColor = AppPalette.Common.White
        }
        override val volume = object : AppColor.ChipColors.GradientColors {
            override val startColor = AppPalette.Unique.Blue
            override val endColor = AppPalette.Unique.Cyan
            override val contentColor = AppPalette.Common.White
        }
        override val repetitions = object : AppColor.ChipColors.GradientColors {
            override val startColor = AppPalette.Unique.Purple
            override val endColor = AppPalette.Unique.Violet
            override val contentColor = AppPalette.Common.White
        }
    }

    public override val example: AppColor.ExampleColors = object : AppColor.ExampleColors {
        override val category: AppColor.ExampleColors.CategoryColors =
            object : AppColor.ExampleColors.CategoryColors {
                override val compound: Color = AppPalette.Unique.Red
                override val isolation: Color = AppPalette.Unique.Orange
            }

        override val weightType: AppColor.ExampleColors.WeightTypeColors =
            object : AppColor.ExampleColors.WeightTypeColors {
                override val free: Color = AppPalette.Unique.Green
                override val fixed: Color = AppPalette.Unique.Emerald
                override val bodyWeight: Color = AppPalette.Unique.Teal
            }

        override val forceType: AppColor.ExampleColors.ForceTypeColors =
            object : AppColor.ExampleColors.ForceTypeColors {
                override val pull: Color = AppPalette.Unique.Sky
                override val push: Color = AppPalette.Unique.Blue
                override val hinge: Color = AppPalette.Unique.Indigo
            }
    }

    public override val profile: AppColor.ProfileColors = object : AppColor.ProfileColors {

        override val experience: AppColor.ProfileColors.ExperienceColors =
            object : AppColor.ProfileColors.ExperienceColors {
                override val beginner: Color = AppPalette.Unique.Purple
                override val intermediate: Color = AppPalette.Unique.Violet
                override val advanced: Color = AppPalette.Unique.Magenta
                override val pro: Color = AppPalette.Unique.Burgundy
            }
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val active = AppPalette.Unique.Green
        override val inactive = AppPalette.NeutralDark.N300.copy(alpha = 0.90f)
        override val background = AppPalette.NeutralDark.N200
        override val outline = AppPalette.NeutralDark.N150
    }

    override val charts: AppColor.Charts = object : AppColor.Charts {
        override val sparkline = object : AppColor.Charts.SparklineColors {
            override val lineA = AppPalette.Blue.P400
            override val lineB = AppPalette.Unique.Green
            override val fillBase = AppPalette.Blue.P400
            override val dot = AppPalette.Blue.P400
            override val min = AppPalette.Unique.Green
            override val max = AppPalette.Unique.Green
        }
        override val area = object : AppColor.Charts.AreaColors {
            override val lineA = AppPalette.Unique.Green
            override val lineB = AppPalette.Blue.P400
            override val fillBase = AppPalette.Unique.Green
            override val glow = AppPalette.Unique.Green
            override val dot = AppPalette.Unique.Green
        }
        override val heatmap = object : AppColor.Charts.HeatmapColors {
            override val missingCell = AppPalette.Common.White.copy(alpha = 0.08f)
        }
        override val radar = object : AppColor.Charts.RadarColors {
            override val strokeFallback = AppPalette.Blue.P500
        }
        override val progress = object : AppColor.Charts.ProgressColors {
            override val track = AppPalette.Common.White.copy(alpha = 0.08f)
        }
    }

    override val palette: AppColor.PaletteColors = object : AppColor.PaletteColors {
        override val palette12BlueWave: List<Color> =
            AppPalette.Gradient.Palette12BlueWave
        override val palette7BlueGrowth: List<Color> =
            AppPalette.Gradient.Palette7BlueGrowth
        override val palette20ColorfulRandom: List<Color> =
            AppPalette.Gradient.Palette20ColorfulRandom
        override val palette5OrangeRedGrowth: List<Color> =
            AppPalette.Gradient.Palette5OrangeRedGrowth
    }

    private fun progressIndicatorColors(color: Color): AppColor.LineIndicatorColors.IndicatorColors =
        object : AppColor.LineIndicatorColors.IndicatorColors {
            override val indicator: Color = color
            override val track: Color = color.copy(alpha = 0.2f)
        }
}
