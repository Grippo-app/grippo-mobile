package com.grippo.design.resources

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public data object AppDp {
    private val padding: Padding = Padding
    private val size: Size = Size
    private val radius: Radius = Radius
    private val icon: Icon = Icon

    private data object Padding {
        val none: Dp = 0.dp
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
        val none: Dp = 0.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val xLarge: Dp = 24.dp
        val xxLarge: Dp = 28.dp
    }

    private data object Icon {
        val tiny: Dp = 12.dp
        val small: Dp = 18.dp
        val medium: Dp = 22.dp
        val large: Dp = 32.dp
        val huge: Dp = 48.dp
    }

    val bottomSheet: BottomSheet = BottomSheet
    val contentPadding: ContentPadding = ContentPadding
    val screen: Screen = Screen
    val input: Input = Input
    val button: Button = Button
    val error: Error = Error
    val loader: Loader = Loader
    val segment: Segment = Segment
    val tab: Tab = Tab
    val menu: Menu = Menu
    val selectableCard: SelectableCard = SelectableCard
    val multiSelectableCard: MultiSelectableCard = MultiSelectableCard
    val informationCard: InformationCard = InformationCard
    val wheelPicker: WheelPicker = WheelPicker
    val chart: Chart = Chart
    val userCard: UserCard = UserCard
    val bodyDetails: BodyDetails = BodyDetails
    val overviewCard: OverviewCard = OverviewCard
    val equipmentCard: EquipmentCard = EquipmentCard
    val exerciseExampleBundlesCard: ExerciseExampleBundlesCard = ExerciseExampleBundlesCard
    val timeline: Timeline = Timeline
    val timeLabel: TimeLabel = TimeLabel
    val datePicker: DatePicker = DatePicker
    val periodPicker: PeriodPicker = PeriodPicker
    val exerciseCard: ExerciseCard = ExerciseCard
    val iterationCard: IterationCard = IterationCard
    val chip: Chip = Chip
    val connectionSnackbar: ConnectionSnackbar = ConnectionSnackbar
    val toggle: Toggle = Toggle
    val radio: Radio = Radio
    val themeCard: ThemeCard = ThemeCard

    public data object Screen {
        val toolbar: Toolbar = Toolbar
        val horizontalPadding: Dp = padding.extraLarge
        val verticalPadding: Dp = padding.extraLarge

        public data object Toolbar {
            val height: Dp = size.medium
        }
    }

    public data object ContentPadding {
        val block: Dp = padding.extraLarge
        val content: Dp = padding.medium
        val subContent: Dp = padding.small
        val text: Dp = padding.extraSmall
    }

    public data object BottomSheet {
        val radius: Dp = AppDp.radius.xxLarge
    }

    public data object Input {
        val height: Dp = size.medium
        val horizontalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.large
        val icon: Dp = AppDp.icon.medium
    }

    public data object Button {
        val height: Dp = size.medium
        val horizontalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.large
        val icon: Dp = AppDp.icon.medium
        val space: Dp = padding.small
        val spaceTransparent: Dp = padding.tiny
    }

    public data object Menu {
        val item: Item = Item
        val radius: Dp = AppDp.radius.large

        public data object Item {
            val horizontalPadding: Dp = padding.large
            val verticalPadding: Dp = padding.large
            val icon: Dp = AppDp.icon.medium
        }
    }

    public data object UserCard {
        val horizontalPadding: Dp = padding.extraLarge
        val verticalPadding: Dp = padding.extraLarge
        val radius: Dp = AppDp.radius.xLarge
    }

    public data object BodyDetails {
        val horizontalPadding: Dp = padding.extraSmall
        val verticalPadding: Dp = padding.extraSmall
        val radius: Dp = AppDp.radius.small
        val icon: Dp = AppDp.icon.medium
    }

    public data object OverviewCard {
        val horizontalPadding: Dp = padding.small
        val verticalPadding: Dp = padding.small
        val icon: Dp = AppDp.icon.small
    }

    public data object DatePicker {
        val icon: Dp = AppDp.icon.medium
        val spacer: Dp = padding.small
    }

    public data object PeriodPicker {
        val icon: Dp = AppDp.icon.medium
        val spacer: Dp = padding.small
    }

    public data object TimeLabel {
        val icon: Dp = AppDp.icon.medium
        val spacer: Dp = padding.extraSmall
    }

    public data object Timeline {
        val dot: Dp = 10.dp
    }

    public data object IterationCard {
        val horizontalPadding: Dp = 4.dp
        val verticalPadding: Dp = 5.dp
    }

    public data object InformationCard {
        val height: Dp = size.medium
    }

    public data object Toggle {
        val width: Dp = 51.dp
        val height: Dp = 31.dp
        val radius: Dp = 18.dp
        val thumbRadius: Dp = 13.5.dp
        val horizontalPadding: Dp = 16.dp
    }

    public data object Radio {
        val size: Dp = 31.dp
        val borderWidth: Dp = 2.dp
        val innerCircleRadius: Dp = 7.75.dp
    }

    public data object ExerciseCard {
        val radius: Dp = AppDp.radius.large
        val icon: Dp = AppDp.icon.medium
    }

    public data object EquipmentCard {
        val icon: Dp = size.xLarge
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.medium
        val radius: Dp = AppDp.radius.large
    }

    public data object ExerciseExampleBundlesCard

    public data object MultiSelectableCard {
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

    public data object SelectableCard {
        val large: Large = Large

        public data object Large {
            val horizontalPadding: Dp = padding.large
            val verticalPadding: Dp = padding.large
            val radius: Dp = AppDp.radius.large
            val icon: Dp = AppDp.icon.large
        }
    }

    public data object Error {
        val icon: Dp = AppDp.icon.huge
    }

    public data object Loader {
        val icon: Dp = AppDp.icon.large
    }

    public data object Segment {
        val radius: Dp = AppDp.radius.medium
        val height: Dp = size.medium
        val horizontalPadding: Dp = padding.large
    }

    public data object Tab {
        val horizontalPadding: Dp = AppDp.padding.large
        val verticalPadding: Dp = AppDp.padding.small
        val padding: Dp = AppDp.padding.extraSmall
        val icon: Dp = AppDp.icon.medium
    }

    public data object ConnectionSnackbar {
        val horizontalPadding: Dp = AppDp.padding.extraLarge
        val verticalPadding: Dp = AppDp.padding.small
        val padding: Dp = AppDp.padding.extraSmall
        val icon: Dp = AppDp.icon.small
    }

    public data object WheelPicker {
        val height: Dp = size.medium * 3
        val radius: Dp = AppDp.radius.large
    }

    public data object ThemeCard {
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.xxLarge
    }

    public data object Chart {
        val pie: Pie = Pie

        public data object Pie {
            val width: Dp = size.tiny
        }
    }

    public data object Chip {
        val horizontalPadding: Dp = padding.medium
        val verticalPadding: Dp = padding.small
        val radius: Dp = AppDp.radius.large
        val trailingSize: Dp = icon.small
        val spaceBetween: Dp = padding.small
    }
}
