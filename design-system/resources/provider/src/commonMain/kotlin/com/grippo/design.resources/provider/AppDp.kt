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
        val extraLarge: Dp = 20.dp
    }

    private data object Size {
        val tiny: Dp = 24.dp
        val small: Dp = 32.dp
        val medium: Dp = 50.dp
        val large: Dp = 64.dp
        val xLarge: Dp = 72.dp
        val xxLarge: Dp = 96.dp
    }

    private data object Radius {
        val tiny: Dp = 4.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 28.dp
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

    val bottomSheet: BottomSheet = BottomSheet
    val contentPadding: ContentPadding = ContentPadding
    val screen: Screen = Screen
    val dialog: Dialog = Dialog
    val input: Input = Input
    val button: Button = Button
    val error: Error = Error
    val confirmation: Confirmation = Confirmation
    val loader: Loader = Loader
    val segment: Segment = Segment
    val tab: Tab = Tab
    val menu: Menu = Menu
    val checkSelectableCard: CheckSelectableCard = CheckSelectableCard
    val toggleSelectableCard: ToggleSelectableCard = ToggleSelectableCard
    val wheelPicker: WheelPicker = WheelPicker
    val userCard: UserCard = UserCard
    val achievementCard: AchievementCard = AchievementCard
    val equipmentCard: EquipmentCard = EquipmentCard
    val timeline: Timeline = Timeline
    val timeLabel: TimeLabel = TimeLabel
    val exerciseCard: ExerciseCard = ExerciseCard
    val exerciseExampleCard: ExerciseExampleCard = ExerciseExampleCard
    val exerciseExampleImage: ExerciseExampleImage = ExerciseExampleImage
    val iterationCard: IterationCard = IterationCard
    val iterationsCard: IterationsCard = IterationsCard
    val chip: Chip = Chip
    val connectionSnackbar: ConnectionSnackbar = ConnectionSnackbar
    val toggle: Toggle = Toggle
    val suggestionCard: SuggestionCard = SuggestionCard
    val digest: Digest = Digest
    val calendar: Calendar = Calendar
    val performanceTrend: PerformanceTrend = PerformanceTrend
    val metrics: Metrics = Metrics
    val empty: Empty = Empty

    public data object Screen {
        val toolbar: Toolbar = Toolbar
        val horizontalPadding: Dp = padding.extraLarge
        val verticalPadding: Dp = padding.extraLarge

        public data object Toolbar {
            val height: Dp = size.small
        }
    }

    public data object ContentPadding {
        val block: Dp = padding.extraLarge
        val content: Dp = padding.medium
        val subContent: Dp = padding.small
        val text: Dp = padding.extraSmall
    }

    public data object BottomSheet {
        val radius: Dp = AppDp.radius.large
        val toolbar: Toolbar = Toolbar

        public data object Toolbar {
            val height: Dp = size.small
        }
    }

    public data object Dialog {
        val top: Dp = padding.small
        val bottom: Dp = padding.extraLarge
        val horizontalPadding: Dp = padding.extraLarge
    }

    public data object SuggestionCard {
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.medium
    }

    public data object Calendar {
        val monthly: Monthly = Monthly

        public data object Monthly {
            val cellHeight: Dp = size.xxLarge
        }
    }

    public data object Digest {
        val content: Content = Content
        val layout: Layout = Layout

        public data object Layout {
            val radius: Dp = AppDp.radius.small
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.small
        }

        public data object Content {
            val radius: Dp = AppDp.radius.small
            val horizontalPadding: Dp = padding.small
            val verticalPadding: Dp = padding.small
            val icon: Dp = AppDp.icon.large
        }
    }

    public data object PerformanceTrend {
        val icon: Dp = AppDp.icon.extraSmall
    }

    public data object Metrics {
        val highlights: Highlights = Highlights
        val panel: Panel = Panel
        val status: Status = Status
        val distribution: Distribution = Distribution
        val volume: Volume = Volume
        val strength: Strength = Strength
        val lastTraining: LastTraining = LastTraining
        val trainingSummary: TrainingSummary = TrainingSummary
        val trainingProfile: TrainingProfile = TrainingProfile

        public data object Highlights {
            val icon: Dp = AppDp.icon.medium
        }

        public data object Digests {
            val icon: Dp = AppDp.icon.medium
        }

        public data object Status {
            val radius: Dp = AppDp.radius.small
            val horizontalPadding: Dp = padding.small
            val verticalPadding: Dp = padding.extraSmall
        }

        public data object Panel {
            val small: Small = Small
            val large: Large = Large

            public data object Small {
                val radius: Dp = AppDp.radius.small
                val horizontalPadding: Dp = padding.medium
                val verticalPadding: Dp = padding.medium
                val spacer: Dp = padding.small
            }

            public data object Large {
                val radius: Dp = AppDp.radius.medium
                val horizontalPadding: Dp = padding.large
                val verticalPadding: Dp = padding.large
                val spacer: Dp = padding.medium
            }
        }

        public data object Distribution {
            val size: Dp = icon.xxxLarge
        }

        public data object Volume {
            val height: Dp = icon.xxxLarge
        }

        public data object Strength {
            val height: Dp = icon.xxxLarge
        }

        public data object LastTraining {
            val radius: Dp = AppDp.radius.medium
            val horizontalPadding: Dp = padding.large
            val verticalPadding: Dp = padding.large
            val image: Dp = icon.xxxxLarge
        }

        public data object TrainingProfile {
            val chartSize: Dp = Size.xLarge
        }

        public data object TrainingSummary {
            val spot: Dp = icon.xxxxLarge
        }
    }

    public data object Empty {
        val image: Dp = icon.xxxxLarge
    }

    public data object Input {
        val height: Dp = size.medium
        val radius: Dp = AppDp.radius.medium
        val horizontalPadding: Dp = padding.extraLarge
        val icon: Dp = AppDp.icon.medium
    }

    public data object Button {
        public val small: Small = Small
        public val medium: Medium = Medium

        public data object Medium {
            val height: Dp = size.medium
            val horizontalPadding: Dp = padding.large
            val icon: Dp = AppDp.icon.medium
            val space: Dp = padding.tiny
            val spaceTransparent: Dp = padding.tiny
        }

        public data object Small {
            val height: Dp = size.small
            val horizontalPadding: Dp = padding.medium
            val icon: Dp = AppDp.icon.medium
            val space: Dp = padding.tiny
            val spaceTransparent: Dp = padding.tiny
        }
    }

    public data object Menu {
        val item: Item = Item
        val radius: Dp = AppDp.radius.medium

        public data object Item {
            val horizontalPadding: Dp = padding.large
            val verticalPadding: Dp = padding.large
            val icon: Dp = AppDp.icon.medium
        }
    }

    public data object UserCard

    public data object TimeLabel {
        val icon: Dp = AppDp.icon.large
        val spacer: Dp = padding.extraSmall
    }

    public data object Timeline {
        val dot: Dp = 10.dp
    }

    public data object IterationsCard {
        val radius: Dp = AppDp.radius.medium
    }

    public data object IterationCard {
        val editable: Editable = Editable

        public data object Editable {
            val horizontalPadding: Dp = padding.medium
            val height: Dp = size.medium
            val radius: Dp = AppDp.radius.small
            val spacing: Dp = padding.medium
        }
    }

    public data object Toggle {
        val width: Dp = 48.dp
        val height: Dp = 29.dp
        val radius: Dp = 17.dp
        val thumbRadius: Dp = 12.5.dp
        val horizontalPadding: Dp = 15.dp
    }

    public data object ExerciseCard {
        val large: Large = Large
        val medium: Medium = Medium
        val small: Small = Small

        public data object Large

        public data object Medium

        public data object Small {
            val radius: Dp = AppDp.radius.medium
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.medium
        }
    }

    public data object ExerciseExampleImage {
        val small: Small = Small
        val large: Large = Large
        val medium: Medium = Medium

        public data object Small {
            val size: Dp = icon.xLarge
            val radius: Dp = AppDp.radius.small
        }

        public data object Medium {
            val size: Dp = icon.xxLarge
            val radius: Dp = AppDp.radius.medium
        }

        public data object Large {
            val size: Dp = icon.xxxLarge
            val radius: Dp = AppDp.radius.medium
        }
    }

    public data object ExerciseExampleCard {
        val small: Small = Small
        val medium: Medium = Medium

        public data object Small

        public data object Medium
    }

    public data object EquipmentCard {
        val icon: Dp = size.xLarge
    }

    public data object AchievementCard {
        val icon: Dp = AppDp.icon.large
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.medium
        val radius: Dp = AppDp.radius.medium
        val chip: Chip = Chip
        val emblem: Emblem = Emblem

        public data object Chip {
            val radius: Dp = AppDp.radius.small
            val horizontalPadding: Dp = padding.small
            val verticalPadding: Dp = padding.extraSmall
        }

        public data object Emblem {
            val size: Dp = AppDp.size.small
            val radius: Dp = AppDp.radius.small
            val padding: Dp = AppDp.padding.extraSmall
        }
    }

    public data object ToggleSelectableCard {
        val medium: Medium = Medium
        val small: Small = Small

        public data object Medium {
            val height: Dp = size.medium
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.extraSmall
            val radius: Dp = AppDp.radius.medium
            val icon: Dp = size.large
        }

        public data object Small {
            val height: Dp = size.medium
            val horizontalPadding: Dp = padding.medium
            val radius: Dp = AppDp.radius.medium
        }
    }

    public data object CheckSelectableCard {
        val large: Large = Large
        val medium: Medium = Medium
        val small: Small = Small

        public data object Large {
            val horizontalPadding: Dp = padding.large
            val verticalPadding: Dp = padding.large
            val radius: Dp = AppDp.radius.medium
            val icon: Dp = AppDp.icon.xLarge
        }

        public data object Medium {
            val horizontalPadding: Dp = padding.medium
            val verticalPadding: Dp = padding.large
            val radius: Dp = AppDp.radius.medium
            val icon: Dp = AppDp.icon.large
        }

        public data object Small {
            val verticalPadding: Dp = padding.small
            val horizontalPadding: Dp = padding.small
            val radius: Dp = AppDp.radius.large
            val icon: Dp = AppDp.icon.small
        }
    }

    public data object Error {
        val icon: Dp = AppDp.icon.xxxLarge
    }

    public data object Confirmation {
        val icon: Dp = AppDp.icon.xxxLarge
    }

    public data object Loader {
        val icon: Dp = AppDp.icon.xLarge
    }

    public data object Segment {
        val outline: Outline = Outline
        val fill: Fill = Fill

        public data object Fill {
            val space: Dp = padding.extraSmall
            val inRadius: Dp = radius.small
            val outRadius: Dp = radius.medium
        }

        public data object Outline {
            val height: Dp = size.medium
            val horizontalPadding: Dp = padding.large
        }
    }

    public data object Tab {
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.medium
        val icon: Dp = AppDp.icon.large
    }

    public data object ConnectionSnackbar {
        val horizontalPadding: Dp = padding.extraLarge
        val verticalPadding: Dp = padding.small
    }

    public data object WheelPicker {
        val height: Dp = size.medium * 3
        val radius: Dp = AppDp.radius.medium
    }

    public data object Chip {
        val medium: Medium = Medium
        val small: Small = Small

        public data object Medium {
            val horizontalPadding: Dp = padding.medium
            val radius: Dp = AppDp.radius.medium
            val trailingSize: Dp = icon.small
            val spaceBetween: Dp = padding.small
            val height: Dp = size.small
        }

        public data object Small {
            val horizontalPadding: Dp = padding.small
            val verticalPadding: Dp = padding.extraSmall
            val radius: Dp = AppDp.radius.small
            val trailingSize: Dp = icon.extraSmall
            val spaceBetween: Dp = padding.extraSmall
            val height: Dp = size.tiny
        }
    }
}
