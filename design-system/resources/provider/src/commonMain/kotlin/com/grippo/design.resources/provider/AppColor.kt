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
    public val skeleton: SkeletonColors
    public val icon: IconColors
    public val toggle: ToggleColors
    public val muscle: MuscleColors
    public val segment: SegmentColors
    public val konfetti: Konfetti
    public val chip: ChipColors
    public val theme: ThemeColors
    public val radio: RadioColors
    public val charts: Charts
    public val example: ExampleColors
    public val profile: ProfileColors

    public interface DividerColors {
        public val default: Color
    }

    public interface ButtonColors {
        public val backgroundPrimary: Color
        public val textPrimary: Color
        public val iconPrimary: Color
        public val backgroundPrimaryDisabled: Color
        public val contentPrimaryDisabled: Color

        public val backgroundSecondary: Color
        public val textSecondary: Color
        public val iconSecondary: Color
        public val borderSecondary: Color
        public val backgroundSecondaryDisabled: Color
        public val contentSecondaryDisabled: Color

        public val backgroundTertiary: Color
        public val textTertiary: Color
        public val iconTertiary: Color
        public val borderTertiary: Color
        public val backgroundTertiaryDisabled: Color
        public val contentTertiaryDisabled: Color

        public val contentTransparentDisabled: Color
        public val textTransparent: Color
        public val iconTransparent: Color
    }

    public interface ToggleColors {
        public val checkedThumb: Color
        public val checkedTrack: Color

        public val uncheckedThumb: Color
        public val uncheckedTrack: Color
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

    public interface BackgroundColors {
        public val screen: Color
        public val dialog: Color
        public val card: Color
        public val accent: Color
    }

    public interface DialogColors {
        public val handle: Color
        public val scrim: Color
    }

    public interface TextColors {
        public val primary: Color
        public val secondary: Color
        public val tertiary: Color
        public val inverted: Color
        public val disabled: Color
    }

    public interface SemanticColors {
        public val success: Color
        public val error: Color
        public val warning: Color
        public val info: Color
        public val accent: Color
    }

    public interface OverlayColors {
        public val defaultShadow: Color
    }

    public interface BorderColors {
        public val default: Color
        public val focus: Color
    }

    public interface SkeletonColors {
        public val background: Color
        public val shimmer: Color
    }

    public interface IconColors {
        public val primary: Color
        public val secondary: Color
        public val disabled: Color
        public val accent: Color
        public val inverted: Color
    }

    public interface ProfileColors {
        public val experienceColors: ExperienceColors

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
        public val focused: Color
        public val active: Color
        public val inactive: Color
        public val background: Color
        public val outline: Color

        public val text: Color
        public val palette: List<Color>
        public val scaleStops: List<Pair<Float, Color>>
    }

    public interface SegmentColors {
        public val active: Color
        public val inactive: Color
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

    public interface ThemeColors {
        public val lightText: Color
        public val lightBackground1: Color
        public val lightBackground2: Color
        public val darkText: Color
        public val darkBackground1: Color
        public val darkBackground2: Color
    }

    public interface Charts {
        public val area: AreaColors
        public val categorical: CategoricalColors
        public val heatmap: HeatmapColors
        public val progress: ProgressColors
        public val radar: RadarColors
        public val sparkline: SparklineColors

        public interface SparklineColors {
            public val lineA: Color
            public val lineB: Color
            public val fillBase: Color
            public val dot: Color
            public val min: Color
            public val max: Color
        }

        public interface RadarColors {
            public val strokeFallback: Color
            public val palette: List<Color>
            public val scaleStops: List<Pair<Float, Color>>
        }

        public interface HeatmapColors {
            public val scaleStops: List<Pair<Float, Color>>
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
            public val palette: List<Color>
        }

        public interface CategoricalColors {
            public val palette1: List<Color>
            public val palette2: List<Color>
        }
    }
}