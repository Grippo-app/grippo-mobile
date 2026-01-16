package com.grippo.design.resources.provider

import androidx.compose.ui.graphics.Color

public interface AppColor {
    public val button: ButtonColors
    public val input: InputColors
    public val background: BackgroundColors
    public val dialog: DialogColors
    public val text: TextColors
    public val divider: DividerColors
    public val semantic: SemanticColors
    public val overlay: OverlayColors
    public val border: BorderColors
    public val brand: BrandColors
    public val icon: IconColors
    public val toggle: ToggleColors
    public val muscle: MuscleColors
    public val segment: SegmentColors
    public val konfetti: Konfetti
    public val chip: ChipColors
    public val radio: RadioColors
    public val charts: Charts
    public val example: ExampleColors
    public val profile: ProfileColors
    public val palette: PaletteColors
    public val static: Static
    public val achievements: Achievements
    public val lineIndicator: LineIndicatorColors
    public val selectableCardColors: SelectableCardColors

    public interface DividerColors {
        public val default: Color
    }

    public interface Achievements {
        public val bestTonnage1: Color
        public val bestTonnage2: Color
        public val bestWeight1: Color
        public val bestWeight2: Color
        public val lifetimeVolume1: Color
        public val lifetimeVolume2: Color
        public val maxRepetitions1: Color
        public val maxRepetitions2: Color
        public val peakIntensity1: Color
        public val peakIntensity2: Color
    }

    public interface Static {
        public val white: Color
    }

    public interface ButtonColors {
        public val backgroundPrimary1: Color
        public val backgroundPrimary2: Color
        public val borderPrimary: Color
        public val textPrimary: Color
        public val iconPrimary: Color

        public val backgroundSecondary1: Color
        public val backgroundSecondary2: Color
        public val borderSecondary: Color
        public val textSecondary: Color
        public val iconSecondary: Color

        public val textTertiary: Color
        public val iconTertiary: Color
        public val borderTertiary: Color

        public val textTransparent: Color
        public val iconTransparent: Color

        public val backgroundDisabled: Color
        public val contentDisabled: Color
    }

    public interface ToggleColors {
        public val checkedThumb: Color
        public val checkedTrack1: Color
        public val checkedTrack2: Color

        public val uncheckedThumb: Color
        public val uncheckedTrack1: Color
        public val uncheckedTrack2: Color
    }

    public interface RadioColors {
        public val selectedThumb: Color
        public val selectedTrack: Color

        public val unselectedTrack: Color

        public val disabledSelectedThumb: Color
        public val disabledUnselectedTrack: Color
    }

    public interface InputColors {
        public val placeholder: Color
        public val label: Color
        public val text: Color

        public val leading: Color
        public val trailing: Color

        public val backgroundDisabled: Color
        public val textDisabled: Color
        public val placeholderDisabled: Color
    }

    public interface BrandColors {
        public val color1: Color
        public val color2: Color
        public val color3: Color
        public val color4: Color
        public val color5: Color
        public val color6: Color
    }

    public interface BackgroundColors {
        public val screen: Color
        public val dialog: Color
        public val card: Color
    }

    public interface DialogColors {
        public val scrim: Color
    }

    public interface TextColors {
        public val primary: Color
        public val secondary: Color
        public val tertiary: Color
        public val disabled: Color
    }

    public interface SemanticColors {
        public val success: Color
        public val error: Color
        public val warning: Color
        public val info: Color
    }

    public interface OverlayColors {
        public val defaultShadow: Color
    }

    public interface BorderColors {
        public val default: Color
        public val focus: Color
    }

    public interface IconColors {
        public val primary: Color
        public val secondary: Color
        public val tertiary: Color
        public val disabled: Color
    }

    public interface ProfileColors {
        public val experience: ExperienceColors

        public interface ExperienceColors {
            public val beginner: Color
            public val intermediate: Color
            public val advanced: Color
            public val pro: Color
        }
    }

    public interface ExampleColors {
        public val category: CategoryColors
        public val weightType: WeightTypeColors
        public val forceType: ForceTypeColors

        public interface CategoryColors {
            public val compound: Color
            public val isolation: Color
        }

        public interface WeightTypeColors {
            public val free: Color
            public val fixed: Color
            public val bodyWeight: Color
        }

        public interface ForceTypeColors {
            public val pull: Color
            public val push: Color
            public val hinge: Color
        }
    }

    public interface MuscleColors {
        public val active: Color
        public val inactive: Color
        public val background: Color
        public val outline: Color
        public val palette6MuscleCalm: List<Color>
    }

    public interface SegmentColors {
        public val selector: Color
    }

    public interface Konfetti {
        public val confettiColor1: Color
        public val confettiColor2: Color
        public val confettiColor3: Color
        public val confettiColor4: Color
        public val confettiColor5: Color
        public val confettiColor6: Color
        public val confettiColor7: Color
        public val confettiColor8: Color
        public val confettiColor9: Color
        public val confettiColor10: Color
    }

    public interface SelectableCardColors {
        public val small: Small

        public interface Small {
            public val selectedBackground1: Color
            public val selectedBackground2: Color
        }
    }

    public interface LineIndicatorColors {
        public val primary: IndicatorColors
        public val success: IndicatorColors
        public val info: IndicatorColors
        public val warning: IndicatorColors
        public val muted: IndicatorColors

        public interface IndicatorColors {
            public val indicator: Color
            public val track: Color
        }
    }

    public interface ChipColors {
        public val intensity: GradientColors
        public val volume: GradientColors
        public val repetitions: GradientColors

        public interface GradientColors {
            public val startColor: Color
            public val endColor: Color
            public val contentColor: Color
        }
    }

    public interface Charts {
        public val area: AreaColors
        public val heatmap: HeatmapColors
        public val progress: ProgressColors
        public val bar: BarColors
        public val radar: RadarColors
        public val sparkline: SparklineColors
        public val tooltip: Tooltip

        public interface SparklineColors {
            public val lineA: Color
            public val lineB: Color
            public val fillBase: Color
            public val dot: Color
            public val middle: Color
        }

        public interface BarColors {
            public val focus: Color
            public val guide: Color
        }

        public interface Tooltip {
            public val background: Color
            public val border: Color
            public val text: Color
        }

        public interface RadarColors {
            public val strokeFallback: Color
        }

        public interface HeatmapColors {
            public val missingCell: Color
        }

        public interface AreaColors {
            public val lineA: Color
            public val lineB: Color
            public val fillBase: Color
            public val glow: Color
            public val dot: Color
        }

        public interface ProgressColors {
            public val track: Color
        }
    }

    public interface PaletteColors {
        public val palette5OrangeRedGrowth: List<Color>
        public val palette7BlueGrowth: List<Color>
    }
}
