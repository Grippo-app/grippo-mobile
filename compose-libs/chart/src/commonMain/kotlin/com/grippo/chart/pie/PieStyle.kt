package com.grippo.chart.pie

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class PieStyle(
    val layout: Layout = Layout(),
    val arc: Arc = Arc(),
    val labels: Labels = Labels(),
    val leaders: Leaders = Leaders(),
) {
    @Immutable
    public data class Layout(
        val padding: Dp = 12.dp,
        val startAngleDeg: Float = -90f, // 0° at 3 o'clock; -90° puts first slice at top
    )

    @Immutable
    public data class Arc(
        val width: Dp = 22.dp,          // donut ring width
        val paddingAngleDeg: Float = 2f, // gap between slices
        val cornerRadius: Dp = 6.dp,    // rounded inner/outer corners
    )

    @Immutable
    public data class Labels(
        val insideMinAngleDeg: Float = 14f,        // show inside labels if slice >= this angle
        val outsideMinAngleDeg: Float = 5f,        // otherwise try outside labels with leaders
        val textStyle: TextStyle = TextStyle(color = Color(0xCCFFFFFF)),
        val labelPadding: Dp = 6.dp,
        val formatter: (slice: PieSlice, percentInt: Int) -> String = { s, p ->
            if (p > 0) "${s.label} ${p}%" else s.label
        }
    )

    @Immutable
    public data class Leaders(
        val show: Boolean = true,
        val lineWidth: Dp = 1.dp,
        val offset: Dp = 8.dp, // how far labels sit outside the ring
    )
}