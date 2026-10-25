package com.grippo.design.resources.provider

import androidx.compose.ui.graphics.Color

public interface AppColor {
    public val bottomBar: BottomBarColors
    public val button: ButtonColors
    public val input: InputColors
    public val background: BackgroundColors
    public val dialog: DialogColors
    public val text: TextColors
    public val divider: DividerColors
    public val timeline: TimelineColors
    public val semantic: SemanticColors
    public val overlay: OverlayColors
    public val shimmer: ShimmerColors
    public val skeleton: SkeletonColors
    public val border: BorderColors
    public val illustration: IllustrationColors
    public val bannerCard: BannerCardColors
    public val welcomeChecklist: WelcomeChecklistColors
    public val brand: BrandColors
    public val icon: IconColors
    public val toggle: ToggleColors
    public val muscle: MuscleColors
    public val segment: SegmentColors
    public val konfetti: Konfetti
    public val charts: Charts
    public val example: ExampleColors
    public val profile: ProfileColors
    public val goal: GoalColors
    public val palette: PaletteColors
    public val static: Static
    public val context: ContextColors
    public val selection: SelectionColors
    public val stepProgressIndicator: StepProgressIndicatorColors

    public interface BottomBarColors {
        public val activeContent: Color
        public val inactiveContent: Color
    }

    public interface DividerColors {
        public val default: Color
        public val subtle: Color
    }

    public interface TimelineColors {
        public val marker: Color
        public val markerBackground: Color
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
        public val backgroundError: Color
        public val contentError: Color
        public val backgroundDisabled: Color
        public val contentDisabled: Color
    }

    public interface ToggleColors {
        public val disabledThumb: Color
        public val disabledTrack: Color
        public val checkedThumb: Color
        public val checkedTrack1: Color
        public val checkedTrack2: Color
        public val uncheckedThumb: Color
        public val uncheckedTrack1: Color
        public val uncheckedTrack2: Color
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

    public interface IllustrationColors {
        public val accent: Color
        public val backgroundStart: Color
        public val backgroundEnd: Color
    }

    public interface BannerCardColors {
        public val disabledBackground: Color
        public val illustrationBackground: Color
        public val mutedBackground: Color
    }

    public interface WelcomeChecklistColors {
        public val completedBackground: Color
        public val pendingBackground: Color
    }

    public interface BrandColors {
        public val color1: Color
        public val color2: Color
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
        public val errorBorder: Color
        public val warning: Color
        public val info: Color
        public val notice: Color
        public val decline: Color
        public val successBackground: Color
        public val errorBackground: Color
        public val warningBackground: Color
        public val infoBackground: Color
        public val noticeBackground: Color
        public val declineBackground: Color
    }

    public interface OverlayColors {
        public val shadow: Color
        public val shadowSpotAlphaMultiplier: Float
        public val overlay: Color
    }

    public interface ShimmerColors {
        public val highlight: Color
        public val buttonPrimary: Color
        public val buttonSecondary: Color
        public val buttonTertiary: Color
        public val buttonTransparent: Color
        public val buttonError: Color
    }

    public interface SkeletonColors {
        public val bone: Color
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
        public val backgroundAlpha: Float
    }

    public interface ContextColors {
        public val muscle: Color
        public val equipment: Color
        public val experience: Color
        public val body: Color
        public val volume: Color
        public val intensity: Color
        public val duration: Color
        public val repetitions: Color
    }

    public interface ProfileColors {
        public val avatarShade: Color
        public val avatarBaseAlpha: Float
        public val avatarRingAlpha: Float
        public val experience: ExperienceColors

        public interface ExperienceColors {
            public val beginner: Color
            public val intermediate: Color
            public val advanced: Color
            public val pro: Color
        }
    }

    public interface GoalColors {
        public val accent: Color
        public val background: Color
    }

    public interface ExampleColors {
        public val equipmentCompactHighlight: Color
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
        public val highlight: Color
        public val active: Color
        public val inactive: Color
        public val background: Color
        public val outline: Color
        public val compactBackground: Color
        public val compactInactive: Color
        public val summaryHighlight: Color
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

    public interface SelectionColors {
        public val accent: Color
        public val background: Color
        public val onAccent: Color
        public val selectedCardBackground: Color
        public val selectedCardBorder: Color
        public val checkedIndicator: Color
        public val onCheckedIndicator: Color
        public val onDisabledIndicator: Color
    }

    public interface StepProgressIndicatorColors {
        public val fillStart: Color
        public val fillEnd: Color
        public val track: Color
    }

    public interface Charts {
        public val area: AreaColors
        public val bar: BarColors
        public val progress: ProgressColors
        public val radar: RadarColors
        public val sparkline: SparklineColors
        public val tooltip: TooltipColor
        public val ring: RingColor
        public val indicator: IndicatorColors

        public interface RingColor {
            public val success: RingPalette
            public val info: RingPalette
            public val warning: RingPalette
            public val error: RingPalette
            public val muted: RingPalette

            public interface RingPalette {
                public val indicator: Color
                public val track: Color
            }
        }

        public interface IndicatorColors {
            public val primary: IndicatorColors
            public val success: IndicatorColors
            public val info: IndicatorColors
            public val warning: IndicatorColors
            public val error: IndicatorColors
            public val decline: IndicatorColors
            public val muted: IndicatorColors
            public val before: IndicatorColors

            public interface IndicatorColors {
                public val colors: List<Color>
                public val stops: List<Float>? get() = null
                public val track: Color
                public val dominant: Color get() = colors.last()
            }
        }

        public interface SparklineColors {
            public val lineA: Color
            public val fillStart: Color
            public val fillEnd: Color
        }

        public interface TooltipColor {
            public val background: Color
            public val border: Color
            public val text: Color
            public val focus: Color
            public val guide: Color
        }

        public interface RadarColors {
            public val grid: Color
            public val fillAlpha: Float
        }

        public interface AreaColors {
            public val lineA: Color
            public val fillStart: Color
            public val fillEnd: Color
        }

        public interface ProgressColors {
            public val track: Color
        }

        public interface BarColors {
            public val baseline: Color
        }
    }

    public interface PaletteColors {
        public val palette5OrangeRedGrowth: List<Color>
        public val palette7BlueGrowth: List<Color>
    }
}
