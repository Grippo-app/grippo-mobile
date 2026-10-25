package com.grippo.design.resources.provider

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public data object AppDp {
    private val padding: Padding = Padding
    private val size: Size = Size
    private val radius: Radius = Radius
    private val icon: Icon = Icon

    private data object Padding {
        val tiny: Dp = 2.dp
        val extraSmall: Dp = 4.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val xLarge: Dp = 20.dp
    }

    private data object Size {
        val tiny: Dp = 24.dp
        val small: Dp = 32.dp
        val medium: Dp = 50.dp
        val large: Dp = 64.dp
        val xLarge: Dp = 96.dp
        val xxLarge: Dp = 262.dp
    }

    private data object Radius {
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val xLarge: Dp = 20.dp
        val xxLarge: Dp = 28.dp
    }

    private data object Icon {
        val extraSmall: Dp = 12.dp
        val small: Dp = 18.dp
        val medium: Dp = 22.dp
        val large: Dp = 24.dp
        val xLarge: Dp = 32.dp
        val xxLarge: Dp = 64.dp
        val xxxLarge: Dp = 100.dp
        val xxxxLarge: Dp = 200.dp
    }

    val bottomBar: BottomBar = BottomBar

    public data object BottomBar {
        val actionSize: Dp = 56.dp
        val actionLift: Dp = padding.medium
        val actionSpace: Dp = 72.dp
        val minHeight: Dp = size.large
        val iconSize: Dp = icon.large
        val indicatorWidth: Dp = 56.dp
        val indicatorHeight: Dp = size.small
        val indicatorRadius: Dp = radius.large
        val horizontalPadding: Dp = padding.small
        val verticalPadding: Dp = padding.small
        val gap: Dp = padding.extraSmall
    }

    val bottomSheet: BottomSheet = BottomSheet
    public val divider: Divider = Divider
    val contentPadding: ContentPadding = ContentPadding
    val screen: Screen = Screen
    val muscleSelection: MuscleSelection = MuscleSelection
    public val muscle: Muscle = Muscle
    val dialog: Dialog = Dialog
    val input: Input = Input
    val button: Button = Button
    val loader: Loader = Loader
    val konfetti: Konfetti = Konfetti
    val segment: Segment = Segment
    val menu: Menu = Menu
    val checkSelectableCard: CheckSelectableCard = CheckSelectableCard
    val toggleSelectableCard: ToggleSelectableCard = ToggleSelectableCard
    val wheelPicker: WheelPicker = WheelPicker
    val achievementCard: AchievementCard = AchievementCard
    val equipmentCard: EquipmentCard = EquipmentCard
    val timeline: Timeline = Timeline
    val exerciseCard: ExerciseCard = ExerciseCard
    val exerciseExampleCard: ExerciseExampleCard = ExerciseExampleCard
    val weightHistoryCard: WeightHistoryCard = WeightHistoryCard
    val deleteUserCard: DeleteUserCard = DeleteUserCard
    val exerciseExampleImage: ExerciseExampleImage = ExerciseExampleImage
    val iterationCard: IterationCard = IterationCard
    val iterationsCard: IterationsCard = IterationsCard
    val chip: Chip = Chip
    val toggle: Toggle = Toggle
    val check: Check = Check
    val calendar: Calendar = Calendar
    val metrics: Metrics = Metrics
    public val goal: Goal = Goal
    val brandHeader: BrandHeader = BrandHeader
    val brandSplash: BrandSplash = BrandSplash
    val userCard: UserCard = UserCard
    val welcome: Welcome = Welcome
    val illustration: Illustration = Illustration
    val badge: Badge = Badge
    val bannerCard: BannerCard = BannerCard
    val focusFrame: FocusFrame = FocusFrame
    val tooltip: Tooltip = Tooltip
    val training: Training = Training
    val accent: Accent = Accent
    val stepProgressIndicator: StepProgressIndicator = StepProgressIndicator
    val periodPicker: PeriodPicker = PeriodPicker
    val pagerIndicator: PagerIndicator = PagerIndicator
    val swipe: Swipe = Swipe
    val shadow: Shadow = Shadow

    public data object Divider {
        public val thickness: Dp = 1.dp
    }

    public data object Goal {
        public val markSize: Dp = 40.dp
        public val iconSize: Dp = icon.large
        public val radius: Dp = AppDp.radius.medium
    }

    public data object Swipe {
        val actionWidth: Dp = 56.dp
        val actionHeight: Dp = 48.dp
        val actionRadius: Dp = radius.large
        val actionSpacing: Dp = padding.small
        val actionIconSize: Dp = icon.medium
    }

    public data object Shadow {
        val component: Dp = 2.dp
        val card: Dp = 4.dp
        val container: Dp = 6.dp
    }

    public data object Konfetti {
        val heightFraction: Float = 0.7f
    }

    public data object MuscleSelection {
        val heroMaxHeight: Dp = icon.xxxxLarge
        val heroMinHeight: Dp = 0.dp
        val listMinHeight: Dp = size.xLarge
        val heroHeightFraction: Float = 0.4f
    }

    public data object Muscle {
        public val markSize: Dp = size.small
        public val inlineMarkSize: Dp = 28.dp
        public val summaryMarkSize: Dp = size.small
        public val markRadius: Dp = radius.small
        public val markPadding: Dp = padding.tiny
    }

    public data object Screen {
        val onboardingHeroHeight: Dp = 220.dp
        val onboardingHeroRadius: Dp = radius.xxLarge
        val toolbar: Toolbar = Toolbar
        val horizontalPadding: Dp = padding.xLarge
        val verticalPadding: Dp = padding.xLarge

        public data object Toolbar {
            val progressWidthFraction: Float = 0.5f
            val height: Dp = size.small
        }
    }

    public data object ContentPadding {
        val block: Dp = padding.xLarge
        val content: Dp = padding.medium
        val subContent: Dp = padding.small
        val text: Dp = padding.extraSmall
    }

    public data object BottomSheet {
        val radius: Dp = AppDp.radius.xxLarge
        val toolbar: Toolbar = Toolbar

        public data object Toolbar {
            val height: Dp = 48.dp
        }
    }

    public data object Dialog {
        val maxMessageHeight: Dp = 400.dp
        val top: Dp = padding.small
        val bottom: Dp = padding.xLarge
        val horizontalPadding: Dp = padding.xLarge
    }

    public data object Calendar {
        val trainingBarHeight: Dp = 5.dp
        val monthly: Monthly = Monthly

        public data object Monthly {
            val cellHeight: Dp = size.xLarge
        }
    }

    public data object Metrics {
        val charts: Charts = Charts

        public data object Charts {
            val axis: Axis = Axis
            val peek: Peek = Peek
            val area: Area = Area
            val bar: Bar = Bar
            val pie: Pie = Pie
            val progress: Progress = Progress
            val radar: Radar = Radar
            val ring: Ring = Ring
            val sparkline: Sparkline = Sparkline
            val indicator: Indicator = Indicator

            public data object Axis {
                val strokeWidth: Dp = 1.dp
            }

            public data object Peek {
                val hitSlop: Dp = 26.dp
                val guideWidth: Dp = Axis.strokeWidth
                val guideDash: Dp = 6.dp
                val guideGap: Dp = guideDash
                val focusRadius: Dp = 3.5.dp
                val focusRingWidth: Dp = 2.dp
                val focusHaloRadius: Dp = 0.dp
                val tooltipRadius: Dp = AppDp.radius.small
                val tooltipHorizontalPadding: Dp = padding.small
                val tooltipVerticalPadding: Dp = padding.extraSmall
                val tooltipMargin: Dp = 6.dp
                val tooltipBorderWidth: Dp = Axis.strokeWidth
                val tooltipLabelSpacing: Dp = padding.tiny
            }

            public data object Area {
                val axisLabelGap: Dp = padding.small
                val lineWidth: Dp = 2.5.dp
            }

            public data object Bar {
                val axisLabelSpacing: Dp = padding.small
                val radius: Dp = 4.dp
                val maxWidth: Dp = 40.dp
                val widthFraction: Float = 0.6f
                val selectionStrokeWidth: Dp = Axis.strokeWidth
            }

            public data object Pie {
                val layoutPadding: Dp = padding.small
                val arcWidth: Dp = 24.dp
                val radius: Dp = 6.dp
                val labelPadding: Dp = 6.dp
                val leaderWidth: Dp = Axis.strokeWidth
                val leaderOffset: Dp = padding.small
            }

            public data object Progress {
                val barHeight: Dp = 16.dp
                val spacing: Dp = padding.medium
                val radius: Dp = 10.dp
                val labelPadding: Dp = padding.small
            }

            public data object Radar {
                val labelPadding: Dp = 3.dp
                val seriesWidth: Dp = 1.5.dp
                val dotRadius: Dp = 2.dp
                val focusRingWidth: Dp = Axis.strokeWidth
                val focusHaloRadius: Dp = 10.dp
            }

            public data object Ring {
                val strokeWidth: Dp = 12.dp
            }

            public data object Sparkline {
                val axisLabelSpacing: Dp = padding.small
                val verticalPadding: Dp = padding.small
                val lineWidth: Dp = Area.lineWidth
            }

            public data object Indicator {
                val height: Dp = 6.dp
                val labelSpacing: Dp = 6.dp
            }
        }

        val panel: Panel = Panel
        val status: Status = Status
        val overview: Overview = Overview
        val performance: Performance = Performance
        val profile: Profile = Profile
        val volume: Volume = Volume

        public data object Status {
            val radius: Dp = AppDp.radius.small
            val horizontalPadding: Dp = padding.small
            val verticalPadding: Dp = padding.extraSmall
        }

        public data object Panel {
            val borderWidth: Dp = 1.dp
            val small: Small = Small
            val large: Large = Large

            public data object Small {
                val radius: Dp = AppDp.radius.large
                val horizontalPadding: Dp = padding.medium
                val verticalPadding: Dp = padding.medium
                val spacer: Dp = padding.small
            }

            public data object Large {
                val radius: Dp = AppDp.radius.xLarge
                val horizontalPadding: Dp = padding.large
                val verticalPadding: Dp = padding.large
                val spacer: Dp = padding.medium
            }
        }

        public data object Overview {
            val muscleImageWidth: Dp = 144.dp
            val muscleImageHeight: Dp = 140.dp
            val detailsIcon: Dp = Icon.small
            val rhythmIcon: Dp = icon.xLarge
            val insightCardWidth: Dp = 320.dp
        }

        public data object Performance {
            val strength: Strength = Strength
            val trend: Trend = Trend
            public val records: Records = Records

            public data object Strength {
                val height: Dp = icon.xxxLarge
            }

            public data object Trend {
                val icon: Dp = AppDp.icon.extraSmall
                val historyAspectRatio: Float = 3.2f
            }

            public data object Records {
                public val trophyIcon: Dp = Trend.icon
            }
        }

        public data object Profile {
            val goal: Goal = Goal

            public data object Goal {
                val statusMarker: Dp = 6.dp
                val breakdown: Breakdown = Breakdown

                public data object Breakdown {
                    val radius: Dp = Panel.Small.radius
                    val horizontalPadding: Dp = Panel.Small.horizontalPadding
                    val verticalPadding: Dp = Panel.Small.verticalPadding
                    val severityDot: Dp = padding.small
                }
            }
        }

        public data object Volume {
            val chart: Chart = Chart

            public data object Chart {
                val height: Dp = icon.xxxLarge
                val aspectRatio: Float = 1.7f
                val historyAspectRatio: Float = 3.2f
            }
        }
    }

    public data object BrandHeader {
        val radius: Dp = AppDp.radius.xxLarge
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.large
        val logoSize: Dp = icon.xxLarge
    }

    public data object BrandSplash {
        val logoSize: Dp = icon.xxxLarge
        val glowSize: Dp = 280.dp
    }

    public data object UserCard {
        val card: Card = Card

        public data object Card {
            val radius: Dp = AppDp.radius.xLarge
            val borderWidth: Dp = 1.dp
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.medium
        }

        val detailed: Detailed = Detailed
        val avatar: Avatar = Avatar
        val headerLoading: HeaderLoading = HeaderLoading

        public data object HeaderLoading {
            val nameWidthFraction: Float = 0.4f
            val emailWidthFraction: Float = 0.6f
            val radius: Dp = 4.dp
        }

        public data object Detailed {
            val avatarRing: Dp = 3.dp
        }

        public data object Avatar {
            val small: Dp = size.medium
            val large: Dp = icon.xxLarge
        }
    }

    public data object Welcome {
        val card: Card = Card
        val checkmark: Checkmark = Checkmark
        val fact: Fact = Fact

        public data object Card {
            val borderWidth: Dp = 1.dp
            val radius: Dp = AppDp.radius.xLarge
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.medium
        }

        public data object Checkmark {
            val size: Dp = icon.small
        }

        public data object Fact {
            val icon: Dp = AppDp.icon.small
            val space: Dp = padding.extraSmall
            val gap: Dp = padding.small
        }
    }

    public data object Badge {
        val radius: Dp = AppDp.radius.small
        val minSize: Dp = size.tiny
        val horizontalPadding: Dp = padding.small
        val verticalPadding: Dp = padding.extraSmall
        val iconOnlyPadding: Dp = padding.extraSmall
        val icon: Dp = AppDp.icon.extraSmall
        val spaceBetween: Dp = padding.extraSmall
    }

    public data object BannerCard {
        val iconRadius: Dp = radius.medium
        val space: Dp = padding.medium
        val iconBackground: Dp = 40.dp
        val icon: Dp = AppDp.icon.medium
        val textSpacing: Dp = padding.extraSmall
        val trailingSpacing: Dp = padding.small
    }

    public data object FocusFrame {
        val radius: Dp = AppDp.radius.large
        val borderWidth: Dp = 1.dp
        val horizontalPadding: Dp = padding.medium
        val verticalPadding: Dp = padding.medium
    }

    public data object Tooltip {
        val textSpacing: Dp = padding.tiny
        val radius: Dp = AppDp.radius.medium
        val horizontalPadding: Dp = padding.medium
        val verticalPadding: Dp = padding.small
        val arrowSize: Dp = 8.dp
        val arrowWidth: Dp = 14.dp
        val maxWidth: Dp = 240.dp
        val iconSize: Dp = icon.small
        val spaceBetween: Dp = padding.small
        val borderWidth: Dp = 1.dp
        val elevation: Dp = 8.dp
    }

    public data object Training {
        val radius: Dp = AppDp.radius.medium
        val horizontalPadding: Dp = padding.medium
        val verticalPadding: Dp = padding.small
        val textSpacing: Dp = padding.tiny
        public val exerciseLoadTypeIcon: Dp = 20.dp
        val startTraining: StartTraining = StartTraining
        public val recap: Recap = Recap

        public data object StartTraining {
            val icon: Dp = AppDp.icon.medium
        }

        public data object Recap {
            public val verdict: Verdict = Verdict
            public val loading: Loading = Loading

            public data object Verdict {
                public val space: Dp = BannerCard.space
                public val tile: Tile = Tile

                public data object Tile {
                    public val size: Dp = BannerCard.iconBackground
                    public val radius: Dp = BannerCard.iconRadius
                    public val icon: Dp = BannerCard.icon
                }
            }

            public data object Loading {
                public val title: Title = Title
                public val description: Description = Description
                public val fact: Fact = Fact

                public data object Title {
                    public val widthFraction: Float = 0.6f
                }

                public data object Description {
                    public val widthFraction: Float = 0.9f
                }

                public data object Fact {
                    public val value: Value = Value
                    public val label: Label = Label

                    public data object Value {
                        public val widthFraction: Float = 0.5f
                    }

                    public data object Label {
                        public val widthFraction: Float = 0.7f
                    }
                }
            }
        }
    }

    public data object Illustration {
        val compact: Dp = 128.dp
        val medium: Dp = 180.dp
        val large: Dp = 220.dp
    }

    public data object Input {
        val borderWidth: Dp = 1.dp
        val trailingActionSize: Dp = 40.dp
        val height: Dp = size.medium
        val radius: Dp = AppDp.radius.large
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.extraSmall
        val trailingPadding: Dp = padding.extraSmall
        val unitEndPadding: Dp = padding.small
        val contentSpacing: Dp = padding.small
        val icon: Dp = AppDp.icon.medium
    }

    public data object Button {
        val border: Dp = 1.dp
        public val iconRadius: Dp = radius.small
        val verticalPadding: Dp = padding.extraSmall
        public val small: Small = Small
        public val medium: Medium = Medium

        public data object Medium {
            val height: Dp = size.medium
            public val radius: Dp = AppDp.radius.large
            val horizontalPadding: Dp = padding.medium
            val icon: Dp = AppDp.icon.medium
            val space: Dp = padding.small
            val spaceTransparent: Dp = padding.extraSmall
        }

        public data object Small {
            val height: Dp = size.small
            public val radius: Dp = AppDp.radius.medium
            val horizontalPadding: Dp = padding.medium
            val icon: Dp = AppDp.icon.small
            val space: Dp = padding.small
            val spaceTransparent: Dp = padding.extraSmall
        }
    }

    public data object Menu {
        val borderWidth: Dp = 1.dp
        val contentPadding: Dp = padding.large
        val headerIconContainer: Dp = size.small
        val headerIconRadius: Dp = AppDp.radius.small
        val headerIcon: Dp = AppDp.icon.medium
        val item: Item = Item
        val radius: Dp = AppDp.radius.xLarge

        public data object Item {
            val minHeight: Dp = 56.dp
            val verticalPadding: Dp = padding.medium
            val space: Dp = padding.medium
            val iconContainer: Dp = size.small
            val iconRadius: Dp = AppDp.radius.small
            val valueMaxWidthFraction: Float = 0.45f
            val loadingWidth: Dp = 50.dp
            val loadingHeight: Dp = 18.dp
            val loadingRadius: Dp = 4.dp
            val icon: Dp = AppDp.icon.small
        }
    }

    public data object Timeline {
        val lineWidth: Dp = 2.dp
        val dot: Dp = 10.dp
        val markerHalo: Dp = 3.dp
    }

    public data object IterationsCard {
        val borderWidth: Dp = 1.dp
        val radius: Dp = AppDp.radius.xLarge
    }

    public data object IterationCard {
        val borderWidth: Dp = 1.dp
        val radius: Dp = AppDp.radius.small
        public val numberSize: Dp = size.small
        val historyContextWidth: Dp = 112.dp
        public val editor: Editor = Editor

        public data object Editor {
            public val minActionHeight: Dp = 48.dp
            public val icon: Dp = 20.dp
        }
    }

    public data object Check {
        val size: Dp = icon.large
        val iconSize: Dp = icon.small
        val touchSize: Dp = 48.dp
        val borderWidth: Dp = 1.dp
    }

    public data object Toggle {
        val width: Dp = 48.dp
        val height: Dp = 29.dp
        val radius: Dp = 17.dp
        val thumbRadius: Dp = 12.5.dp
        val horizontalPadding: Dp = 15.dp
    }

    public data object WeightHistoryCard {
        val chartAspectRatio: Float = 2.2f
        val borderWidth: Dp = 1.dp
        val radius: Dp = AppDp.radius.large
        val horizontalPadding: Dp = padding.medium
        val verticalPadding: Dp = padding.medium
    }

    public data object DeleteUserCard {
        val borderWidth: Dp = 1.dp
        val radius: Dp = AppDp.radius.xLarge
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.medium
    }

    public data object ExerciseCard {
        val chevron: Dp = AppDp.icon.small
        val small: Small = Small

        public data object Small {
            val borderWidth: Dp = 1.dp
            val radius: Dp = AppDp.radius.large
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.medium
        }
    }

    public data object ExerciseExampleCard {
        val radius: Dp = AppDp.radius.xLarge
        val borderWidth: Dp = 1.dp
        val contentPadding: Dp = padding.large
    }

    public data object ExerciseExampleImage {
        val borderWidth: Dp = 1.dp
        val small: Small = Small
        public val compact: Compact = Compact
        val large: Large = Large
        val medium: Medium = Medium

        public data object Small {
            val size: Dp = icon.xLarge
            val radius: Dp = AppDp.radius.small
            val placeholderPadding: Dp = padding.small
        }

        public data object Compact {
            public val size: Dp = 48.dp
            public val radius: Dp = AppDp.radius.medium
            public val placeholderPadding: Dp = padding.medium
        }

        public data object Medium {
            val size: Dp = icon.xxLarge
            val radius: Dp = AppDp.radius.large
            val placeholderPadding: Dp = padding.xLarge
        }

        public data object Large {
            val size: Dp = icon.xxxLarge
            val radius: Dp = AppDp.radius.xLarge
            val placeholderPadding: Dp = 32.dp
        }
    }

    public data object EquipmentCard {
        val icon: Dp = size.large
        public val inlineIcon: Dp = AppDp.icon.xLarge
        public val compactIcon: Dp = AppDp.icon.xLarge
        public val compactFreeWeightPadding: Dp = padding.tiny
        val width: Dp = 144.dp
    }

    public data object AchievementCard {
        val borderWidth: Dp = 1.dp
        val width: Dp = 224.dp
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.xLarge
        val emblem: Emblem = Emblem

        public data object Emblem {
            val size: Dp = 48.dp
            val radius: Dp = AppDp.radius.medium
            val padding: Dp = AppDp.padding.medium
        }
    }

    public data object ToggleSelectableCard {
        val borderWidth: Dp = 1.dp
        val medium: Medium = Medium
        val small: Small = Small

        public data object Medium {
            val height: Dp = size.medium
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.medium
            public val radius: Dp = AppDp.radius.large
            val icon: Dp = size.large
        }

        public data object Small {
            val height: Dp = size.medium
            val horizontalPadding: Dp = padding.large
            val verticalPadding: Dp = padding.medium
            public val radius: Dp = AppDp.radius.large
        }
    }

    public data object CheckSelectableCard {
        val borderWidth: Dp = 1.dp
        val large: Large = Large
        val medium: Medium = Medium
        val small: Small = Small

        public data object Large {
            val horizontalPadding: Dp = padding.large
            val verticalPadding: Dp = padding.large
            val radius: Dp = AppDp.radius.xLarge
            val icon: Dp = AppDp.icon.xLarge
        }

        public data object Medium {
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.large
            public val radius: Dp = AppDp.radius.large
        }

        public data object Small {
            val verticalPadding: Dp = padding.small
            val horizontalPadding: Dp = padding.large
            public val radius: Dp = AppDp.radius.medium
            val leadingIcon: Dp = AppDp.icon.medium
        }
    }

    public data object Accent {
        val smallWidth: Dp = padding.extraSmall
    }

    public data object PeriodPicker {
        val icon: Dp = AppDp.icon.medium
    }

    public data object StepProgressIndicator {
        val height: Dp = 6.dp
        val gap: Dp = padding.extraSmall
    }

    public data object PagerIndicator {
        val radius: Dp = AppDp.radius.xLarge
        val borderWidth: Dp = 1.dp
        val dotSize: Dp = 8.dp
        val dotSizeActive: Dp = 24.dp
        val dotSpacing: Dp = padding.small
    }

    public data object Loader {
        val container: Dp = size.large
        val icon: Dp = AppDp.icon.xLarge
    }

    public data object Segment {
        val outline: Outline = Outline
        val fill: Fill = Fill

        public data object Fill {
            val space: Dp = padding.extraSmall
            val inRadius: Dp = radius.medium
            val outRadius: Dp = radius.large
        }

        public data object Outline {
            val indicatorWidth: Dp = 2.dp
            val verticalPadding: Dp = padding.extraSmall
            val height: Dp = size.medium
            val horizontalPadding: Dp = padding.large
        }
    }

    public data object WheelPicker {
        val height: Dp = size.medium * 3
        val radius: Dp = AppDp.radius.large
    }

    public data object Chip {
        val verticalPadding: Dp = padding.extraSmall
        val borderWidth: Dp = 1.dp
        val markerSize: Dp = 8.dp
        val small: Small = Small
        val medium: Medium = Medium

        public data object Small {
            val horizontalPadding: Dp = padding.small
            val radius: Dp = AppDp.radius.small
            val leadingSize: Dp = icon.small
            val spaceBetween: Dp = padding.extraSmall
            val height: Dp = size.tiny
        }

        public data object Medium {
            val horizontalPadding: Dp = padding.medium
            val radius: Dp = AppDp.radius.medium
            val leadingSize: Dp = icon.medium
            val spaceBetween: Dp = padding.small
            val height: Dp = size.small
        }
    }
}
