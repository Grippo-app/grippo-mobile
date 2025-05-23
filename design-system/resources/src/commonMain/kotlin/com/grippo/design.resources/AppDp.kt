package com.grippo.design.resources

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public data object AppDp {
    val paddings: Paddings = Paddings
    val size: Size = Size
    val shape: Shape = Shape
    val icon: Icon = Icon

    public data object Paddings {
        val screenHorizontal: Dp = 20.dp
        val screenVertical: Dp = 20.dp
        val mediumHorizontal: Dp = 16.dp
        val mediumVertical: Dp = 16.dp
        val smallHorizontal: Dp = 12.dp
        val smallVertical: Dp = 8.dp
    }

    public data object Size {
        val smallComponentHeight: Dp = 32.dp
        val mediumComponentHeight: Dp = 50.dp
    }

    public data object Shape {
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
    }

    public data object Icon {
        val small: Dp = 18.dp
        val medium: Dp = 32.dp
        val large: Dp = 48.dp
    }
}