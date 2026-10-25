package com.grippo.chart.pie

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

@Immutable
public data class PieStyle(
    val layout: Layout,
    val arc: Arc,
    val labels: Labels,
    val leaders: Leaders,
) {
    @Immutable
    public data class Layout(
        val padding: Dp,
        val startAngleDeg: Float,
    )

    @Immutable
    public data class Arc(
        val width: Dp,
        val paddingAngleDeg: Float,
        val cornerRadius: Dp,
    )

    @Immutable
    public sealed class Labels {

        @Immutable
        public data class Adaptive(
            val insideMinAngleDeg: Float,
            val outsideMinAngleDeg: Float,
            val textStyle: TextStyle,
            val labelPadding: Dp,
            val formatter: (slice: PieSlice, percentInt: Int) -> String,
        ) : Labels()

        @Immutable
        public data class Inside(
            val textStyle: TextStyle,
            val labelPadding: Dp,
            val formatter: (slice: PieSlice, percentInt: Int) -> String,
        ) : Labels()

        @Immutable
        public data class Outside(
            val textStyle: TextStyle,
            val labelPadding: Dp,
            val formatter: (slice: PieSlice, percentInt: Int) -> String,
        ) : Labels()
    }

    @Immutable
    public data class Leaders(
        val show: Boolean,
        val lineWidth: Dp,
        val offset: Dp,
    )
}