package com.grippo.design.resources

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public data object AppDp {
    val screen: Screen = Screen
    val input: Input = Input
    val button: Button = Button
    val error: Error = Error
    val loader: Loader = Loader
    val segment: Segment = Segment
    val selectableCard: SelectableCard = SelectableCard
    val informationCard: InformationCard = InformationCard
    val wheelPicker: WheelPicker = WheelPicker

    val userCard: UserCard = UserCard
    val bodyDetails: BodyDetails = BodyDetails
    val overviewCard: OverviewCard = OverviewCard

    val exerciseCard: ExerciseCard = ExerciseCard
    val exerciseIterations: ExerciseIterations = ExerciseIterations
    val exerciseDetails: ExerciseDetails = ExerciseDetails

    val menu: Menu = Menu

    val paddings: Paddings = Paddings
    val shape: Shape = Shape
    val icon: Icon = Icon

    public data object Screen {
        val horizontalPadding: Dp = 20.dp
        val verticalPadding: Dp = 20.dp
    }

    public data object Input {
        val height: Dp = 50.dp
        val horizontalPadding: Dp = 16.dp
        val radius: Dp = 16.dp
        val icon: Dp = 18.dp
    }

    public data object Button {
        val height: Dp = 50.dp
        val horizontalPadding: Dp = 16.dp
        val radius: Dp = 16.dp
        val icon: Dp = 18.dp
        val space: Dp = 8.dp
    }

    public data object Menu {
        val item: Item = Item
        val radius: Dp = 16.dp

        public data object Item {
            val horizontalPadding: Dp = 16.dp
            val verticalPadding: Dp = 16.dp
            val icon: Dp = 20.dp
        }
    }

    public data object ExerciseDetails {
        val horizontalPadding: Dp = 16.dp
    }

    public data object ExerciseIterations {
        val horizontalPadding: Dp = 16.dp
    }

    public data object UserCard {
        val horizontalPadding: Dp = 16.dp
        val verticalPadding: Dp = 16.dp
        val radius: Dp = 16.dp
        val icon: Dp = 24.dp
    }

    public data object BodyDetails {
        val horizontalPadding: Dp = 4.dp
        val verticalPadding: Dp = 4.dp
        val radius: Dp = 12.dp
        val icon: Dp = 18.dp
    }

    public data object OverviewCard {
        val horizontalPadding: Dp = 8.dp
        val verticalPadding: Dp = 8.dp
        val radius: Dp = 12.dp
        val icon: Dp = 32.dp
    }

    public data object ExerciseCard {
        val horizontalPadding: Dp = 16.dp
        val verticalPadding: Dp = 16.dp
        val radius: Dp = 16.dp
        val icon: Dp = 24.dp
    }

    public data object InformationCard {
        val height: Dp = 50.dp
    }

    public data object SelectableCard {
        val large: Large = Large
        val medium: Medium = Medium
        val small: Small = Small

        public data object Large {
            val horizontalPadding: Dp = 16.dp
            val verticalPadding: Dp = 16.dp
            val radius: Dp = 16.dp
            val icon: Dp = 32.dp
        }

        public data object Medium {
            val height: Dp = 50.dp
            val horizontalPadding: Dp = 12.dp
            val verticalPadding: Dp = 4.dp
            val radius: Dp = 12.dp
            val icon: Dp = 48.dp
        }

        public data object Small {
            val height: Dp = 50.dp
            val horizontalPadding: Dp = 12.dp
            val radius: Dp = 12.dp
        }
    }

    public data object Error {
        val icon: Dp = 48.dp
    }

    public data object Loader {
        val icon: Dp = 32.dp
    }

    public data object Segment {
        val radius: Dp = 12.dp
        val height: Dp = 50.dp
        val horizontalPadding: Dp = 16.dp
    }

    public data object Paddings {
        val mediumHorizontal: Dp = 16.dp
        val mediumVertical: Dp = 16.dp
    }

    public data object WheelPicker {
        val height: Dp = 50.dp * 3
    }

    public data object Shape {
        val large: Dp = 16.dp
    }

    public data object Icon {
        val xl: Dp = 72.dp
    }
}