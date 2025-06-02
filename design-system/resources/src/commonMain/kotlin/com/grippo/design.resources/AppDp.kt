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
        val circle: Dp = 50.dp
    }

    private data object Icon {
        val tiny: Dp = 12.dp
        val small: Dp = 18.dp
        val medium: Dp = 24.dp
        val large: Dp = 32.dp
        val huge: Dp = 48.dp
    }

    val contentPadding: ContentPadding = ContentPadding
    val screen: Screen = Screen
    val input: Input = Input
    val button: Button = Button
    val error: Error = Error
    val loader: Loader = Loader
    val segment: Segment = Segment
    val menu: Menu = Menu
    val selectableCard: SelectableCard = SelectableCard
    val informationCard: InformationCard = InformationCard
    val wheelPicker: WheelPicker = WheelPicker
    val userCard: UserCard = UserCard
    val bodyDetails: BodyDetails = BodyDetails
    val overviewCard: OverviewCard = OverviewCard
    val equipmentCard: EquipmentCard = EquipmentCard
    val exerciseCard: ExerciseCard = ExerciseCard
    val exerciseIterations: ExerciseIterations = ExerciseIterations
    val exerciseDetails: ExerciseDetails = ExerciseDetails

    public data object Screen {
        val toolbar: Toolbar = Toolbar
        val horizontalPadding: Dp = padding.extraLarge
        val verticalPadding: Dp = padding.extraLarge

        public data object Toolbar {
            val height: Dp = size.medium
        }

        public data object Content {
            val horizontalPadding: Dp = padding.extraLarge
            val verticalPadding: Dp = padding.medium
        }
    }

    public data object ContentPadding {
        val block: Dp = padding.extraLarge
        val content: Dp = padding.medium
        val subContent: Dp = padding.small
        val text: Dp = padding.extraSmall
    }

    public data object Input {
        val height: Dp = size.medium
        val horizontalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.large
        val icon: Dp = AppDp.icon.small
    }

    public data object Button {
        val height: Dp = size.medium
        val horizontalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.large
        val icon: Dp = AppDp.icon.small
        val space: Dp = padding.small
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

    public data object ExerciseDetails {
        val horizontalPadding: Dp = padding.large
    }

    public data object ExerciseIterations {
        val horizontalPadding: Dp = padding.large
    }

    public data object UserCard {
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.large
        val icon: Dp = AppDp.icon.medium
    }

    public data object BodyDetails {
        val horizontalPadding: Dp = padding.extraSmall
        val verticalPadding: Dp = padding.extraSmall
        val radius: Dp = AppDp.radius.medium
        val icon: Dp = AppDp.icon.small
    }

    public data object OverviewCard {
        val horizontalPadding: Dp = padding.small
        val verticalPadding: Dp = padding.small
        val radius: Dp = AppDp.radius.medium
        val icon: Dp = AppDp.icon.large
    }

    public data object ExerciseCard {
        val horizontalPadding: Dp = padding.large
        val verticalPadding: Dp = padding.large
        val radius: Dp = AppDp.radius.large
        val icon: Dp = AppDp.icon.medium
    }

    public data object InformationCard {
        val height: Dp = size.medium
    }

    public data object EquipmentCard {
        val icon: Dp = size.xLarge
        val horizontalPadding: Dp = padding.small
        val verticalPadding: Dp = padding.small
    }

    public data object SelectableCard {
        val large: Large = Large
        val medium: Medium = Medium
        val small: Small = Small

        public data object Large {
            val horizontalPadding: Dp = padding.large
            val verticalPadding: Dp = padding.large
            val radius: Dp = AppDp.radius.large
            val icon: Dp = AppDp.icon.large
        }

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

    public data object WheelPicker {
        val height: Dp = size.medium * 3
        val radius: Dp = AppDp.radius.large
    }
}