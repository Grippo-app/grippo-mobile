package com.grippo.design.resources.provider.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.grippo.design.resources.provider.AppColor

public object DarkColor : AppColor {

    override val bottomBar: AppColor.BottomBarColors = object : AppColor.BottomBarColors {
        override val activeContent = AppPalette.DarkAccent.Orange
        override val inactiveContent = AppPalette.NeutralDark.N650
    }

    override val border: AppColor.BorderColors = object : AppColor.BorderColors {
        override val default = AppPalette.NeutralDark.N300
        override val focus = AppPalette.DarkAccent.Orange
    }

    override val button: AppColor.ButtonColors = object : AppColor.ButtonColors {
        override val backgroundPrimary1 = AppPalette.DarkAccent.Orange
        override val backgroundPrimary2 = lerp(AppPalette.DarkAccent.Orange, AppPalette.DarkAccent.Copper, 0.25f)
        override val borderPrimary = backgroundPrimary2
        override val textPrimary = AppPalette.NeutralDark.N100
        override val iconPrimary = AppPalette.NeutralDark.N100
        override val backgroundSecondary1 = AppPalette.NeutralDark.N300
        override val backgroundSecondary2 = AppPalette.NeutralDark.N300
        override val borderSecondary = AppPalette.NeutralDark.N450
        override val textSecondary = AppPalette.NeutralDark.N800
        override val iconSecondary = AppPalette.NeutralDark.N700
        override val textTertiary: Color = AppPalette.NeutralDark.N800
        override val borderTertiary: Color = Color.Transparent
        override val iconTertiary: Color = AppPalette.NeutralDark.N600
        override val textTransparent = AppPalette.Common.White
        override val iconTransparent = AppPalette.Common.White
        override val backgroundError = AppPalette.DarkAccent.Red
        override val contentError = AppPalette.NeutralDark.N100
        override val backgroundDisabled = AppPalette.NeutralDark.N300
        override val contentDisabled = AppPalette.NeutralDark.N550
    }

    override val icon: AppColor.IconColors = object : AppColor.IconColors {
        override val primary = AppPalette.Common.White
        override val secondary = AppPalette.NeutralDark.N600
        override val tertiary = AppPalette.NeutralDark.N650
        override val disabled = AppPalette.NeutralDark.N300
        override val backgroundAlpha = 0.12f
    }

    override val toggle: AppColor.ToggleColors = object : AppColor.ToggleColors {
        override val disabledThumb = AppPalette.NeutralDark.N550
        override val disabledTrack = AppPalette.NeutralDark.N300
        override val checkedThumb = AppPalette.NeutralDark.N100
        public override val checkedTrack1 = AppPalette.NeutralDark.N800
        public override val checkedTrack2 = AppPalette.NeutralDark.N800
        override val uncheckedThumb = AppPalette.Common.White
        override val uncheckedTrack1 = AppPalette.NeutralDark.N200
        override val uncheckedTrack2 = AppPalette.NeutralDark.N300
    }

    override val divider: AppColor.DividerColors = object : AppColor.DividerColors {
        override val default = AppPalette.NeutralDark.N250
        override val subtle = border.default.copy(alpha = 0.4f)
    }

    override val input: AppColor.InputColors = object : AppColor.InputColors {
        override val placeholder = AppPalette.NeutralDark.N650
        override val label = AppPalette.NeutralDark.N650
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
        override val card = AppPalette.NeutralDark.N200
    }

    override val timeline: AppColor.TimelineColors = object : AppColor.TimelineColors {
        override val marker = AppPalette.DarkAccent.Orange
        override val markerBackground = lerp(background.screen, marker, 0.18f)
    }

    override val goal: AppColor.GoalColors = object : AppColor.GoalColors {
        override val accent = AppPalette.DarkAccent.Copper
        override val background = lerp(DarkColor.background.card, accent, 0.14f)
    }

    override val illustration: AppColor.IllustrationColors = object : AppColor.IllustrationColors {
        override val accent = AppPalette.DarkAccent.Orange
        override val backgroundStart = AppPalette.DarkAccent.Orange.copy(alpha = 0.16f)
        override val backgroundEnd = AppPalette.DarkAccent.Red.copy(alpha = 0.04f)
    }

    override val bannerCard: AppColor.BannerCardColors = object : AppColor.BannerCardColors {
        override val disabledBackground = AppPalette.NeutralDark.N300.copy(alpha = 0.14f)
        override val illustrationBackground = AppPalette.DarkAccent.Orange.copy(alpha = 0.14f)
        override val mutedBackground = AppPalette.NeutralDark.N650.copy(alpha = 0.14f)
    }

    override val welcomeChecklist: AppColor.WelcomeChecklistColors = object : AppColor.WelcomeChecklistColors {
        override val completedBackground = AppPalette.DarkAccent.Green.copy(alpha = 0.18f)
        override val pendingBackground = AppPalette.DarkAccent.Yellow.copy(alpha = 0.18f)
    }

    override val brand: AppColor.BrandColors = object : AppColor.BrandColors {
        override val color1: Color = AppPalette.DarkAccent.Magenta
        override val color2: Color = AppPalette.DarkAccent.Coral
        override val color4: Color = AppPalette.DarkAccent.Teal
        override val color5: Color = AppPalette.DarkAccent.Sky
        override val color6: Color = AppPalette.DarkAccent.Indigo
    }

    override val dialog: AppColor.DialogColors = object : AppColor.DialogColors {
        override val scrim = AppPalette.Common.Black.copy(alpha = 0.70f)
    }

    override val static: AppColor.Static = object : AppColor.Static {
        override val white: Color = AppPalette.Common.White
    }

    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppPalette.NeutralDark.N800
        override val secondary = AppPalette.NeutralDark.N700
        override val tertiary = AppPalette.NeutralDark.N600
        override val disabled = AppPalette.NeutralDark.N400
    }

    override val semantic: AppColor.SemanticColors = object : AppColor.SemanticColors {
        override val success = AppPalette.DarkAccent.Green
        override val error = AppPalette.DarkAccent.Red
        override val errorBorder = error.copy(alpha = 0.5f)
        override val warning = AppPalette.DarkAccent.Orange
        override val info = AppPalette.DarkAccent.Blue
        override val notice = AppPalette.DarkAccent.Yellow
        override val decline = AppPalette.DarkAccent.Indigo
        override val successBackground = lerp(background.card, success, 0.14f)
        override val errorBackground = lerp(background.card, error, 0.14f)
        override val warningBackground = lerp(background.card, warning, 0.14f)
        override val infoBackground = lerp(background.card, info, 0.14f)
        override val noticeBackground = lerp(background.card, notice, 0.14f)
        override val declineBackground = lerp(background.card, decline, 0.14f)
        override val neutralBackground = lerp(background.card, AppPalette.NeutralDark.N650, 0.08f)
    }

    override val shimmer: AppColor.ShimmerColors = object : AppColor.ShimmerColors {
        override val neutral = AppPalette.Common.White.copy(alpha = 0.05f)
        override val accent = AppPalette.Common.Black.copy(alpha = 0.07f)
    }

    override val skeleton: AppColor.SkeletonColors = object : AppColor.SkeletonColors {
        override val bone = AppPalette.Common.White.copy(alpha = 0.05f)
    }

    override val overlay: AppColor.OverlayColors = object : AppColor.OverlayColors {
        override val shadowSpotAlphaMultiplier: Float = 1.5f
        override val fadeAlpha: Float = 0.9f
        override val shadow = AppPalette.Common.Black.copy(alpha = 0.2f)
    }

    override val segment: AppColor.SegmentColors = object : AppColor.SegmentColors {
        override val selector = AppPalette.DarkAccent.Orange
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

    override val stepProgressIndicator: AppColor.StepProgressIndicatorColors =
        object : AppColor.StepProgressIndicatorColors {
            override val fillStart = AppPalette.DarkAccent.Orange
            override val fillEnd = lerp(fillStart, AppPalette.DarkAccent.Copper, 0.25f)
            override val track = AppPalette.NeutralDark.N550
        }

    override val selection: AppColor.SelectionColors =
        object : AppColor.SelectionColors {
            override val accent = AppPalette.DarkAccent.Orange
            override val background = lerp(DarkColor.background.card, accent, 0.10f)
            public override val selectedCardBackground = AppPalette.NeutralDark.N300
            public override val selectedCardBorder = AppPalette.NeutralDark.N650
            public override val checkedIndicator = AppPalette.NeutralDark.N800
            public override val onCheckedIndicator = AppPalette.NeutralDark.N100
            public override val onDisabledIndicator = AppPalette.NeutralDark.N650
        }

    override val context: AppColor.ContextColors = object : AppColor.ContextColors {
        override val muscle: Color = AppPalette.DarkAccent.Coral
        override val equipment: Color = AppPalette.DarkAccent.Green
        override val body: Color = AppPalette.DarkAccent.Sky
        override val volume: Color = AppPalette.DarkAccent.Blue
        override val repetitions: Color = AppPalette.DarkAccent.Purple
        override val intensity: Color = AppPalette.DarkAccent.Green
    }

    public override val example: AppColor.ExampleColors = object : AppColor.ExampleColors {
        override val category: AppColor.ExampleColors.CategoryColors =
            object : AppColor.ExampleColors.CategoryColors {
                override val compound: Color = AppPalette.DarkAccent.Red
                override val isolation: Color = AppPalette.DarkAccent.Orange
            }

        override val weightType: AppColor.ExampleColors.WeightTypeColors =
            object : AppColor.ExampleColors.WeightTypeColors {
                override val free: Color = AppPalette.DarkAccent.Green
                override val fixed: Color = AppPalette.DarkAccent.Emerald
                override val bodyWeight: Color = AppPalette.DarkAccent.Teal
            }

        override val forceType: AppColor.ExampleColors.ForceTypeColors =
            object : AppColor.ExampleColors.ForceTypeColors {
                override val pull: Color = AppPalette.DarkAccent.Sky
                override val push: Color = AppPalette.DarkAccent.Blue
                override val hinge: Color = AppPalette.DarkAccent.Indigo
            }
    }

    public override val profile: AppColor.ProfileColors = object : AppColor.ProfileColors {
        override val avatarBaseAlpha: Float = 0.95f
        override val avatarRingAlpha: Float = 0.35f
        override val avatarShade: Color = AppPalette.NeutralDark.N100.copy(alpha = 0.45f)

        override val experience: AppColor.ProfileColors.ExperienceColors =
            object : AppColor.ProfileColors.ExperienceColors {
                override val beginner: Color = AppPalette.Unique.Purple
                override val intermediate: Color = AppPalette.Unique.Violet
                override val advanced: Color = AppPalette.Unique.Magenta
                override val pro: Color = AppPalette.Unique.Burgundy
            }
    }

    override val muscle: AppColor.MuscleColors = object : AppColor.MuscleColors {
        override val highlight = AppPalette.NeutralDark.N700
        override val inactive = AppPalette.NeutralDark.N400.copy(alpha = 0.90f)
        override val background = AppPalette.NeutralDark.N300
        override val outline = Color.Transparent
        override val compact: AppColor.MuscleColors.CompactColors =
            object : AppColor.MuscleColors.CompactColors {
                override val background = AppPalette.NeutralDark.N450
                override val inactive = AppPalette.NeutralDark.N500
            }
        override val palette6MuscleCalm: List<Color> = AppPalette.Gradient.Palette6MuscleDark
    }

    override val equipment: AppColor.EquipmentColors = object : AppColor.EquipmentColors {
        override val compact: AppColor.EquipmentColors.CompactColors =
            object : AppColor.EquipmentColors.CompactColors {
                override val highlight = lerp(AppPalette.NeutralDark.N700, AppPalette.NeutralDark.N800, 0.5f)
            }
    }

    override val charts: AppColor.Charts = object : AppColor.Charts {
        override val bar = object : AppColor.Charts.BarColors {
            override val baseline = border.default.copy(alpha = 0.45f)
        }
        override val sparkline = object : AppColor.Charts.SparklineColors {
            override val lineA = AppPalette.Blue.P400
            override val fillStart = AppPalette.Blue.P400.copy(alpha = 0.16f)
            override val fillEnd = AppPalette.Blue.P400.copy(alpha = 0f)
        }
        override val tooltip = object : AppColor.Charts.TooltipColor {
            override val background: Color = AppPalette.NeutralDark.N400
            override val border: Color = AppPalette.NeutralDark.N450
            override val text: Color = AppPalette.Common.White
            override val focus: Color = AppPalette.Blue.P400
            override val guide: Color = divider.default
        }
        override val area = object : AppColor.Charts.AreaColors {
            override val lineA = AppPalette.Blue.P400
            override val fillStart = AppPalette.Blue.P400.copy(alpha = 0.16f)
            override val fillEnd = AppPalette.Blue.P400.copy(alpha = 0f)
        }
        override val radar = object : AppColor.Charts.RadarColors {
            override val fillAlpha: Float = 0.16f
            override val grid = AppPalette.NeutralDark.N300
        }
        override val progress = object : AppColor.Charts.ProgressColors {
            override val track = AppPalette.Common.White.copy(alpha = 0.08f)
        }
        override val ring = object : AppColor.Charts.RingColor {
            override val success = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.DarkAccent.Emerald
                override val track: Color = AppPalette.DarkAccent.Emerald.copy(alpha = 0.2f)
            }
            override val info = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.DarkAccent.Blue
                override val track: Color = AppPalette.NeutralDark.N300
            }
            override val warning = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.DarkAccent.Orange
                override val track: Color = AppPalette.DarkAccent.Orange.copy(alpha = 0.2f)
            }
            override val error = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.DarkAccent.Red
                override val track: Color = AppPalette.DarkAccent.Red.copy(alpha = 0.2f)
            }
            override val muted = object : AppColor.Charts.RingColor.RingPalette {
                override val indicator: Color = AppPalette.NeutralDark.N600
                override val track: Color = AppPalette.NeutralDark.N500.copy(alpha = 0.2f)
            }
        }
        override val indicator = object : AppColor.Charts.IndicatorColors {
            override val primary = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.Common.White,
                    AppPalette.DarkAccent.Orange,
                )
                override val track: Color = AppPalette.Common.White.copy(alpha = 0.2f)
            }

            override val success = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.DarkAccent.Green,
                    AppPalette.DarkAccent.Olive
                )
                override val track: Color = AppPalette.DarkAccent.Emerald.copy(alpha = 0.2f)
            }

            override val info = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.DarkAccent.Sky,
                    AppPalette.DarkAccent.Blue,
                )
                override val track: Color = AppPalette.NeutralDark.N500
            }

            override val warning = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.DarkAccent.Yellow,
                    AppPalette.DarkAccent.Orange,
                )
                override val track: Color = AppPalette.DarkAccent.Orange.copy(alpha = 0.2f)
            }

            override val error = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    AppPalette.DarkAccent.Orange,
                    AppPalette.DarkAccent.Red,
                )
                override val track: Color = AppPalette.DarkAccent.Orange.copy(alpha = 0.2f)
            }

            override val decline = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(
                    lerp(AppPalette.DarkAccent.Indigo, AppPalette.Common.White, 0.25f),
                    AppPalette.DarkAccent.Indigo,
                )
                override val track: Color = AppPalette.DarkAccent.Indigo.copy(alpha = 0.2f)
            }

            override val muted = object : AppColor.Charts.IndicatorColors.IndicatorColors {
                override val colors: List<Color> = listOf(AppPalette.NeutralDark.N500)
                override val track: Color = AppPalette.NeutralDark.N500.copy(alpha = 0.2f)
            }
        }
        override val comparison = object : AppColor.Charts.ComparisonColors {
            override val track: Color = AppPalette.NeutralDark.N250
            override val shared: Color = AppPalette.NeutralDark.N600
            override val lossAlpha: Float = 0.5f
            override val marker: Color = AppPalette.NeutralDark.N800
        }
    }

    override val palette: AppColor.PaletteColors = object : AppColor.PaletteColors {
        override val palette7BlueGrowth: List<Color> =
            AppPalette.Gradient.Palette7BlueGrowth
    }
}
