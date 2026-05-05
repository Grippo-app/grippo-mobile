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
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = AppPalette.Common.White
        override val checkedTrack1 = AppPalette.Unique.Green
        override val checkedTrack2 = AppPalette.Unique.Green
        override val uncheckedThumb = AppPalette.Common.White
        override val uncheckedTrack1 = AppPalette.NeutralDark.N200
        override val uncheckedTrack2 = AppPalette.NeutralDark.N300
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val default = AppPalette.NeutralDark.N250
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
        override val dialog = AppPalette.NeutralDark.N100
        override val card = AppPalette.NeutralDark.N250.copy(0.35f)
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
        override val scrim = Color(0xB3000000)
    }

    override val static: AppColor.Static = object : AppColor.Static {
        override val white: Color = AppPalette.Common.White
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppPalette.NeutralDark.N800
        override val secondary = AppPalette.NeutralDark.N700
        override val tertiary = AppPalette.NeutralDark.N500
        override val disabled = AppPalette.NeutralDark.N400
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = AppPalette.Unique.Green
        override val error = AppPalette.Unique.Red
        override val warning = AppPalette.Unique.Orange
        override val info = AppPalette.Common.White
        override val notice = AppPalette.Unique.Yellow
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val shadow = AppPalette.Common.Black.copy(alpha = 0.2f)
        override val overlay = AppPalette.NeutralDark.N200.copy(alpha = 0.8f)
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
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

    override val training: AppColor.TrainingColors = object : AppColor.TrainingColors {
        override val intensity = object : AppColor.TrainingColors.GradientColors {
            override val startColor = AppPalette.Unique.Green
            override val contentColor = AppPalette.Common.White
        }
        override val volume = object : AppColor.TrainingColors.GradientColors {
            override val startColor = AppPalette.Unique.Blue
            override val contentColor = AppPalette.Common.White
        }
        override val repetitions = object : AppColor.TrainingColors.GradientColors {
            override val startColor = AppPalette.Unique.Purple
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
        override val inactive = AppPalette.NeutralDark.N400.copy(alpha = 0.90f)
        override val background = AppPalette.NeutralDark.N300.copy(0.70f)
        override val outline = Color.Transparent
        override val palette6MuscleCalm: List<Color> = AppPalette.Gradient.Palette6MuscleCalm
    }

    override val charts: AppColor.Charts = object : AppColor.Charts {
        override val sparkline = object : AppColor.Charts.SparklineColors {
            override val lineA = AppPalette.Blue.P400
            override val lineB = AppPalette.Unique.Green
            override val fillBase = AppPalette.Blue.P400
        }
        override val tooltip = object : AppColor.Charts.TooltipColor {
            override val background: Color = AppPalette.NeutralDark.N400
            override val border: Color = AppPalette.NeutralDark.N450
            override val text: Color = AppPalette.Common.White
            override val focus: Color = AppPalette.Blue.P400
            override val guide: Color = AppPalette.Blue.P400
        }
        override val area = object : AppColor.Charts.AreaColors {
            override val lineA = AppPalette.Unique.Green
            override val lineB = AppPalette.Blue.P400
            override val fillBase = AppPalette.Unique.Green
            override val glow = AppPalette.Unique.Green
            override val dot = AppPalette.Unique.Green
        }
        override val radar = object : AppColor.Charts.RadarColors {
            override val grid = AppPalette.NeutralDark.N300
        }
        override val progress = object : AppColor.Charts.ProgressColors {
            override val track = AppPalette.Common.White.copy(alpha = 0.08f)
        }
        override val ring = object : AppColor.Charts.RingColor {
            override val success = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.Unique.Emerald
                override val track: Color = AppPalette.Unique.Emerald.copy(alpha = 0.2f)
            }
            override val info = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.Common.White
                override val track: Color = AppPalette.NeutralDark.N300
            }
            override val warning = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.Unique.Orange
                override val track: Color = AppPalette.Unique.Orange.copy(alpha = 0.2f)
            }
            override val error = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.Unique.Red
                override val track: Color = AppPalette.Unique.Red.copy(alpha = 0.2f)
            }
            override val muted = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.NeutralDark.N500
                override val track: Color = AppPalette.NeutralDark.N500.copy(alpha = 0.2f)
            }
        }
        override val indicator = object : AppColor.Charts.IndicatorColors {
            // Brand accent — neutral start, warm finish. Fits hero-progress (goals, milestones).
            override val primary = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.Common.White,
                    AppPalette.Unique.Orange,
                )
                override val track: Color = AppPalette.Common.White.copy(alpha = 0.2f)
            }

            // Achievement / positive — soft green ramps into deep emerald.
            override val success = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.Unique.Green,
                    AppPalette.Unique.Olive
                )
                override val track: Color = AppPalette.Unique.Emerald.copy(alpha = 0.2f)
            }

            // Neutral informational — pastel-blue to brand blue.
            override val info = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.Common.White,
                    AppPalette.Common.White,
                )
                override val track: Color = AppPalette.NeutralDark.N500
            }

            // Hot ramp — yellow attention fading into orange urgency.
            override val warning = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.Unique.Yellow,
                    AppPalette.Unique.Orange,
                )
                override val track: Color = AppPalette.Unique.Orange.copy(alpha = 0.2f)
            }

            override val error = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.Unique.Orange,
                    AppPalette.Unique.Red,
                )
                override val track: Color = AppPalette.Unique.Orange.copy(alpha = 0.2f)
            }

            // Muted — kept solid on purpose; used where progress should fade into the background.
            override val muted = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(AppPalette.NeutralDark.N500)
                override val track: Color = AppPalette.NeutralDark.N500.copy(alpha = 0.2f)
            }
        }
    }

    override val palette: AppColor.PaletteColors = object : AppColor.PaletteColors {
        override val palette7BlueGrowth: List<Color> =
            AppPalette.Gradient.Palette7BlueGrowth
        override val palette5OrangeRedGrowth: List<Color> =
            AppPalette.Gradient.Palette5OrangeRedGrowth
    }
}
