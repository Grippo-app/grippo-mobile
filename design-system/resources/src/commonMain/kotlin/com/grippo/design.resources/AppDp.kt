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

        val componentHorizontal: Dp = 16.dp
        val componentVertical: Dp = 16.dp
    }

    public data object Size {
        val componentHeight: Dp = 50.dp
    }

    public data object Shape {
        val screen: Dp = 24.dp
        val component: Dp = 16.dp
    }

    public data object Icon {
        val component: Dp = 20.dp
    }
}