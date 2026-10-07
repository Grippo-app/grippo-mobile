package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.provider.AppColor

public object LightColor : AppColor {

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = AppPalette.NeutralLight.N250
        override val focus = AppPalette.LightAccent.Blue
    }

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary1 = AppPalette.LightAccent.Orange
        override val backgroundPrimary2 = AppPalette.LightAccent.Red
        override val borderPrimary = AppPalette.LightAccent.Coral
        override val textPrimary = AppPalette.Common.White
        override val iconPrimary = AppPalette.Common.White

        override val backgroundSecondary1 = AppPalette.NeutralLight.N800
        override val backgroundSecondary2 = AppPalette.NeutralLight.N700
        override val borderSecondary = AppPalette.NeutralLight.N700
        override val textSecondary = AppPalette.Common.White
        override val iconSecondary = AppPalette.Common.White

        override val textTertiary: Color = AppPalette.NeutralLight.N800
        override val borderTertiary: Color = Color.Transparent
        override val iconTertiary: Color = AppPalette.NeutralLight.N600

        override val textTransparent = AppPalette.NeutralLight.N800
        override val iconTransparent = AppPalette.NeutralLight.N700

        override val backgroundDisabled = AppPalette.NeutralLight.N200
        override val contentDisabled = AppPalette.NeutralLight.N450
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = AppPalette.NeutralLight.N800
        override val secondary = AppPalette.NeutralLight.N550
        override val tertiary = AppPalette.NeutralLight.N500
        override val disabled = AppPalette.NeutralLight.N400
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val checkedThumb = AppPalette.Common.White
        override val checkedTrack1 = AppPalette.LightAccent.Green
        override val checkedTrack2 = AppPalette.LightAccent.Green
        override val uncheckedThumb = AppPalette.Common.White
        override val uncheckedTrack1 = AppPalette.NeutralLight.N300
        override val uncheckedTrack2 = AppPalette.NeutralLight.N400
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val default = AppPalette.NeutralLight.N250
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val placeholder = AppPalette.NeutralLight.N500
        override val label = AppPalette.NeutralLight.N500
        override val text = AppPalette.NeutralLight.N800
        override val leading = AppPalette.NeutralLight.N600
        override val trailing = AppPalette.NeutralLight.N600
        override val backgroundDisabled = AppPalette.NeutralLight.N200
        override val textDisabled = AppPalette.NeutralLight.N500
        override val placeholderDisabled = AppPalette.NeutralLight.N500
    }

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val screen = AppPalette.NeutralLight.N100
        override val dialog = AppPalette.NeutralLight.N050
        override val card = AppPalette.NeutralLight.N150
    }

    override val brand: AppColor.BrandColors = object : AppColor.BrandColors {
        override val color1: Color = AppPalette.LightAccent.Magenta
        override val color2: Color = AppPalette.LightAccent.Coral

        override val color3: Color = AppPalette.LightAccent.Green
        override val color4: Color = AppPalette.LightAccent.Teal

        override val color5: Color = AppPalette.LightAccent.Sky
        override val color6: Color = AppPalette.LightAccent.Indigo
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val scrim = AppPalette.Common.Black.copy(alpha = 0.40f)
    }

    override val static: AppColor.Static = object : AppColor.Static {
        override val white: Color = AppPalette.Common.White
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppPalette.NeutralLight.N800
        override val secondary = AppPalette.NeutralLight.N700
        override val tertiary = AppPalette.NeutralLight.N500
        override val disabled = AppPalette.NeutralLight.N400
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = AppPalette.LightAccent.Green
        override val error = AppPalette.LightAccent.Red
        override val warning = AppPalette.LightAccent.Orange
        override val info = AppPalette.LightAccent.Blue
        override val notice = AppPalette.LightAccent.Yellow
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val shadow = AppPalette.Common.Black.copy(alpha = 0.12f)
        override val overlay = AppPalette.NeutralLight.N100.copy(alpha = 0.90f)
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val selector = AppPalette.LightAccent.Blue
    }

    override val konfetti: AppColor.Konfetti = object : AppColor.Konfetti {
        override val confettiColor1 = AppPalette.LightAccent.Green
        override val confettiColor2 = AppPalette.LightAccent.Orange
        override val confettiColor3 = AppPalette.LightAccent.Red
        override val confettiColor4 = AppPalette.LightAccent.Blue
        override val confettiColor5 = AppPalette.Blue.P500
        override val confettiColor6 = AppPalette.Blue.P600
        override val confettiColor7 = AppPalette.Blue.P700
        override val confettiColor8 = AppPalette.NeutralLight.N500
        override val confettiColor9 = AppPalette.NeutralLight.N600
        override val confettiColor10 = AppPalette.NeutralLight.N700
    }

    override val selectableCardColors: AppColor.SelectableCardColors =
        object : AppColor.SelectableCardColors {
            override val small: AppColor.SelectableCardColors.Small =
                object : AppColor.SelectableCardColors.Small {
                    override val selectedBackground1: Color = AppPalette.Blue.P600
                    override val selectedBackground2: Color = AppPalette.Blue.P500
                }
        }

    override val context: AppColor.ContextColors = object : AppColor.ContextColors {
        // Profile
        override val muscle: Color = AppPalette.LightAccent.Coral
        override val goal: Color = AppPalette.LightAccent.Magenta
        override val equipment: Color = AppPalette.LightAccent.Green
        override val experience: Color = AppPalette.LightAccent.Teal
        override val body: Color = AppPalette.LightAccent.Sky

        // Training
        override val volume: Color = AppPalette.LightAccent.Blue
        override val repetitions: Color = AppPalette.LightAccent.Purple
        override val intensity: Color = AppPalette.LightAccent.Green
        override val duration: Color = AppPalette.LightAccent.Copper
    }

    public override val example: AppColor.ExampleColors = object : AppColor.ExampleColors {
        override val category: AppColor.ExampleColors.CategoryColors =
            object : AppColor.ExampleColors.CategoryColors {
                override val compound: Color = AppPalette.LightAccent.Red
                override val isolation: Color = AppPalette.LightAccent.Orange
            }

        override val weightType: AppColor.ExampleColors.WeightTypeColors =
            object : AppColor.ExampleColors.WeightTypeColors {
                override val free: Color = AppPalette.LightAccent.Green
                override val fixed: Color = AppPalette.LightAccent.Emerald
                override val bodyWeight: Color = AppPalette.LightAccent.Teal
            }

        override val forceType: AppColor.ExampleColors.ForceTypeColors =
            object : AppColor.ExampleColors.ForceTypeColors {
                override val pull: Color = AppPalette.LightAccent.Sky
                override val push: Color = AppPalette.LightAccent.Blue
                override val hinge: Color = AppPalette.LightAccent.Indigo
            }
    }

    public override val profile: AppColor.ProfileColors = object : AppColor.ProfileColors {
        override val avatarShade: Color = AppPalette.NeutralLight.N800.copy(alpha = 0.12f)

        override val experience: AppColor.ProfileColors.ExperienceColors =
            object : AppColor.ProfileColors.ExperienceColors {
                override val beginner: Color = AppPalette.LightAccent.Purple
                override val intermediate: Color = AppPalette.LightAccent.Violet
                override val advanced: Color = AppPalette.LightAccent.Magenta
                override val pro: Color = AppPalette.LightAccent.Burgundy
            }
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val active = AppPalette.LightAccent.Green
        override val inactive = AppPalette.NeutralLight.N300
        override val background = AppPalette.NeutralLight.N200
        override val outline = AppPalette.NeutralLight.N400
        override val palette6MuscleCalm: List<Color> = AppPalette.Gradient.Palette6MuscleLight
    }

    override val charts: AppColor.Charts = object : AppColor.Charts {
        override val sparkline = object : AppColor.Charts.SparklineColors {
            override val lineA = AppPalette.LightAccent.Blue
            override val lineB = AppPalette.LightAccent.Green
            override val fillBase = AppPalette.LightAccent.Blue
        }
        override val tooltip = object : AppColor.Charts.TooltipColor {
            override val background: Color = AppPalette.NeutralLight.N050
            override val border: Color = AppPalette.NeutralLight.N250
            override val text: Color = AppPalette.NeutralLight.N800
            override val focus: Color = AppPalette.LightAccent.Blue
            override val guide: Color = AppPalette.LightAccent.Blue
        }
        override val area = object : AppColor.Charts.AreaColors {
            override val lineA = AppPalette.LightAccent.Green
            override val lineB = AppPalette.LightAccent.Blue
            override val fillBase = AppPalette.LightAccent.Green
            override val glow = AppPalette.LightAccent.Green
            override val dot = AppPalette.LightAccent.Green
        }
        override val radar = object : AppColor.Charts.RadarColors {
            override val grid = AppPalette.NeutralLight.N250
        }
        override val progress = object : AppColor.Charts.ProgressColors {
            override val track = AppPalette.NeutralLight.N250
        }
        override val ring = object : AppColor.Charts.RingColor {
            override val success = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.LightAccent.Emerald
                override val track: Color = AppPalette.LightAccent.Emerald.copy(alpha = 0.2f)
            }
            override val info = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.LightAccent.Blue
                override val track: Color = AppPalette.NeutralLight.N200
            }
            override val warning = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.LightAccent.Orange
                override val track: Color = AppPalette.LightAccent.Orange.copy(alpha = 0.2f)
            }
            override val error = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.LightAccent.Red
                override val track: Color = AppPalette.LightAccent.Red.copy(alpha = 0.2f)
            }
            override val muted = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.NeutralLight.N500
                override val track: Color = AppPalette.NeutralLight.N250.copy(alpha = 0.2f)
            }
        }
        override val indicator = object : AppColor.Charts.IndicatorColors {
            // Warm brand accents remain visible against light chart surfaces.
            override val primary = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.LightAccent.Coral,
                    AppPalette.LightAccent.Orange,
                )
                override val track: Color = AppPalette.LightAccent.Orange.copy(alpha = 0.16f)
            }

            // Positive progress uses a readable green ramp.
            override val success = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.LightAccent.Green,
                    AppPalette.LightAccent.Olive
                )
                override val track: Color = AppPalette.LightAccent.Emerald.copy(alpha = 0.2f)
            }

            // Informational progress stays blue throughout the ramp.
            override val info = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.LightAccent.Sky,
                    AppPalette.LightAccent.Blue,
                )
                override val track: Color = AppPalette.NeutralLight.N250
            }

            // Warm warning ramp uses deeper tones on light surfaces.
            override val warning = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.LightAccent.Yellow,
                    AppPalette.LightAccent.Orange,
                )
                override val track: Color = AppPalette.LightAccent.Orange.copy(alpha = 0.2f)
            }

            override val error = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.LightAccent.Orange,
                    AppPalette.LightAccent.Red,
                )
                override val track: Color = AppPalette.LightAccent.Orange.copy(alpha = 0.2f)
            }

            // Muted — kept solid on purpose; used where progress should fade into the background.
            override val muted = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(AppPalette.NeutralLight.N500)
                override val track: Color = AppPalette.NeutralLight.N250.copy(alpha = 0.2f)
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
