package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.provider.AppColor

public object DarkColor : AppColor {

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = AppPalette.NeutralDark.N250
        override val focus = AppPalette.NeutralDark.N600
    }

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary1 = AppPalette.Common.White
        override val backgroundPrimary2 = AppPalette.Common.White
        override val borderPrimary = AppPalette.Common.White
        override val textPrimary = AppPalette.Common.Black
        override val iconPrimary = AppPalette.Common.Black
        override val backgroundPrimaryDisabled = AppPalette.NeutralDark.N300
        override val contentPrimaryDisabled = AppPalette.NeutralDark.N550

        override val backgroundSecondary1 = Color.Transparent
        override val backgroundSecondary2 = Color.Transparent
        override val textSecondary = AppPalette.Common.White
        override val iconSecondary = AppPalette.Common.White
        override val borderSecondary = AppPalette.Common.White
        override val backgroundSecondaryDisabled = AppPalette.NeutralDark.N300
        override val contentSecondaryDisabled = AppPalette.NeutralDark.N550

        override val backgroundTertiary1: Color = AppPalette.NeutralDark.N150
        override val backgroundTertiary2: Color = AppPalette.NeutralDark.N150
        override val textTertiary: Color = AppPalette.NeutralDark.N800
        override val borderTertiary: Color = Color.Transparent
        override val iconTertiary: Color = AppPalette.NeutralDark.N600
        override val backgroundTertiaryDisabled: Color = AppPalette.NeutralDark.N300
        override val contentTertiaryDisabled: Color = AppPalette.NeutralDark.N500

        override val textTransparent = AppPalette.Common.White
        override val iconTransparent = AppPalette.Common.White
        override val contentTransparentDisabled = AppPalette.NeutralDark.N550
    }

    override val aiSuggestion: AppColor.AiSuggestion = object : AppColor.AiSuggestion {
        override val background1: Color = AppPalette.Unique.Orange
        override val background2: Color = AppPalette.Unique.Red
        override val border: Color = AppPalette.Unique.Coral
        override val content: Color = AppPalette.Common.White
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = AppPalette.NeutralDark.N700
        override val secondary = AppPalette.NeutralDark.N600
        override val tertiary = AppPalette.NeutralDark.N500
        override val disabled = AppPalette.NeutralDark.N300
        override val accent = AppPalette.Primary.P500
        override val inverted = AppPalette.Common.Black
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = AppPalette.Common.White
        override val checkedTrack1 = AppPalette.Primary.P500
        override val checkedTrack2 = AppPalette.Primary.P600
        override val uncheckedThumb = AppPalette.Common.White
        override val uncheckedTrack1 = AppPalette.NeutralDark.N200
        override val uncheckedTrack2 = AppPalette.NeutralDark.N300
    }

    override val radio: AppColor.RadioColors = object : AppColor.RadioColors {
        override val selectedThumb = AppPalette.Primary.P500
        override val selectedTrack = AppPalette.Primary.P500
        override val unselectedTrack = AppPalette.NeutralDark.N300
        override val disabledSelectedThumb = AppPalette.NeutralDark.N300
        override val disabledUnselectedTrack = AppPalette.NeutralDark.N300
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val default = AppPalette.NeutralDark.N200
    }

    override val achievements: AppColor.Achievements = object : AppColor.Achievements {
        override val bestTonnage1: Color = AppPalette.Unique.Green
        override val bestTonnage2: Color = AppPalette.Unique.Teal
        override val bestWeight1: Color = AppPalette.Unique.Sky
        override val bestWeight2: Color = AppPalette.Unique.Navy
        override val lifetimeVolume1: Color = AppPalette.Unique.Purple
        override val lifetimeVolume2: Color = AppPalette.Unique.Violet
        override val maxRepetitions1: Color = AppPalette.Unique.Brown
        override val maxRepetitions2: Color = AppPalette.Unique.Burgundy
        override val peakIntensity1: Color = AppPalette.Unique.Cyan
        override val peakIntensity2: Color = AppPalette.Unique.Indigo
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
        override val accent = AppPalette.Primary.P500
        override val inverted = AppPalette.NeutralLight.N100
    }

    override val brand: AppColor.BrandColors = object : AppColor.BrandColors {
        override val color1: Color = AppPalette.Unique.Magenta
        override val color2: Color = AppPalette.Unique.Coral

        override val color3: Color = AppPalette.Unique.Green
        override val color4: Color = AppPalette.Unique.Teal

        override val color5: Color = AppPalette.Unique.Sky
        override val color6: Color = AppPalette.Unique.Navy
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val handle = AppPalette.NeutralDark.N400
        override val scrim = Color(0xB3000000)
    }

    override val static: AppColor.Static = object : AppColor.Static {
        override val white: Color = AppPalette.Common.White
        override val black: Color = AppPalette.Common.Black
        override val neutralDarkN150: Color = AppPalette.NeutralDark.N150
        override val neutralDarkN800: Color = AppPalette.NeutralDark.N800
        override val neutralLightN800: Color = AppPalette.NeutralLight.N800
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
        override val info = AppPalette.Primary.P500
        override val accent = AppPalette.Primary.P500
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val defaultShadow = AppPalette.Common.Black.copy(alpha = 0.2f)
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val active = AppPalette.NeutralDark.N800
        override val inactive = AppPalette.NeutralDark.N500
        override val selector = AppPalette.Primary.P500
    }

    override val konfetti: AppColor.Konfetti = object : AppColor.Konfetti {
        override val confettiColor1 = AppPalette.Unique.Green
        override val confettiColor2 = AppPalette.Unique.Orange
        override val confettiColor3 = AppPalette.Unique.Red
        override val confettiColor4 = AppPalette.Primary.P400
        override val confettiColor5 = AppPalette.Primary.P500
        override val confettiColor6 = AppPalette.Primary.P600
        override val confettiColor7 = AppPalette.Primary.P700
        override val confettiColor8 = AppPalette.NeutralDark.N500
        override val confettiColor9 = AppPalette.NeutralDark.N600
        override val confettiColor10 = AppPalette.NeutralDark.N700
    }

    override val selectableCardColors: AppColor.SelectableCardColors =
        object : AppColor.SelectableCardColors {
            override val small: AppColor.SelectableCardColors.Small =
                object : AppColor.SelectableCardColors.Small {
                    override val selectedBackground1: Color = AppPalette.Primary.P600
                    override val selectedBackground2: Color = AppPalette.Primary.P500
                }
        }

    override val chip: AppColor.ChipColors = object : AppColor.ChipColors {
        override val intensity = object : AppColor.ChipColors.GradientColors {
            override val startColor = AppPalette.Unique.Red
            override val endColor = AppPalette.Unique.Orange
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
        override val timer = object : AppColor.ChipColors.GradientColors {
            override val startColor = AppPalette.Unique.Olive
            override val endColor = AppPalette.Unique.Cyan
            override val contentColor = AppPalette.Common.White
        }
    }

    public override val example: AppColor.ExampleColors = object : AppColor.ExampleColors {

        override val category: AppColor.ExampleColors.CategoryColors =
            object : AppColor.ExampleColors.CategoryColors {
                override val compound: Color = AppPalette.Unique.Sky
                override val isolation: Color = AppPalette.Unique.Indigo
            }

        override val weightType: AppColor.ExampleColors.WeightTypeColors =
            object : AppColor.ExampleColors.WeightTypeColors {
                override val free: Color = AppPalette.Unique.Coral
                override val fixed: Color = AppPalette.Unique.Orange
                override val bodyWeight: Color = AppPalette.Unique.Green
            }

        override val forceType: AppColor.ExampleColors.ForceTypeColors =
            object : AppColor.ExampleColors.ForceTypeColors {
                override val pull: Color = AppPalette.Unique.Teal
                override val push: Color = AppPalette.Unique.Red
                override val hinge: Color = AppPalette.Unique.Purple
            }
    }

    public override val profile: AppColor.ProfileColors = object : AppColor.ProfileColors {

        override val experience: AppColor.ProfileColors.ExperienceColors =
            object : AppColor.ProfileColors.ExperienceColors {
                override val beginner: Color = AppPalette.Unique.Green
                override val intermediate: Color = AppPalette.Unique.Orange
                override val advanced: Color = AppPalette.Unique.Red
                override val pro: Color = AppPalette.Unique.Navy
            }
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val focused = AppPalette.Primary.P600
        override val active = AppPalette.Primary.P500
        override val inactive = AppPalette.NeutralDark.N400
        override val background = AppPalette.NeutralDark.N300
        override val outline = AppPalette.NeutralDark.N400
        override val text = AppPalette.Common.White
    }

    override val charts: AppColor.Charts = object : AppColor.Charts {
        override val sparkline = object : AppColor.Charts.SparklineColors {
            override val lineA = AppPalette.Primary.P400
            override val lineB = AppPalette.Unique.Green
            override val fillBase = AppPalette.Primary.P400
            override val dot = AppPalette.Primary.P400
            override val min = AppPalette.Unique.Green
            override val max = AppPalette.Unique.Green
        }
        override val area = object : AppColor.Charts.AreaColors {
            override val lineA = AppPalette.Unique.Green
            override val lineB = AppPalette.Primary.P400
            override val fillBase = AppPalette.Unique.Green
            override val glow = AppPalette.Unique.Green
            override val dot = AppPalette.Unique.Green
        }
        override val heatmap = object : AppColor.Charts.HeatmapColors {
            override val missingCell = AppPalette.Common.White.copy(alpha = 0.08f)
        }
        override val radar = object : AppColor.Charts.RadarColors {
            override val strokeFallback = AppPalette.Primary.P500
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
        override val palette18ColorfulRandom: List<Color> =
            AppPalette.Gradient.Palette18ColorfulRandom
        override val palette5OrangeRedGrowth: List<Color> =
            AppPalette.Gradient.Palette5OrangeRedGrowth
    }
}