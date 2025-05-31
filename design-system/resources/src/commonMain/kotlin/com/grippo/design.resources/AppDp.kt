package com.grippo.design.resources

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public data object AppDp {
    val screen: Screen = Screen
    val input: Input = Input
    val button: Button = Button

    val paddings: Paddings = Paddings
    val size: Size = Size
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
    }

    public data object Button {
        val height: Dp = 50.dp
        val horizontalPadding: Dp = 16.dp
        val radius: Dp = 16.dp
        val icon: Dp = 18.dp
        val space: Dp = 8.dp
    }

    public data object Paddings {
        val mediumHorizontal: Dp = 16.dp
        val mediumVertical: Dp = 16.dp
        val smallHorizontal: Dp = 12.dp
        val smallVertical: Dp = 8.dp
    }

    public data object Size {
        val componentHeight: Dp = 50.dp
    }

    public data object Shape {
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
    }

    public data object Icon {
        val xs: Dp = 18.dp
        val s: Dp = 22.dp
        val m: Dp = 32.dp
        val l: Dp = 48.dp
        val xl: Dp = 72.dp
    }
}