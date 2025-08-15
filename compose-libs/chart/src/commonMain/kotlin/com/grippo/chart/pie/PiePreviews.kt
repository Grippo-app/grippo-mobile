package com.grippo.chart.pie

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun PreviewPieBasic() {
    val data = PieData(
        slices = listOf(
            PieSlice(id = "a", label = "A", value = 30f, color = Color(0xFF6AA9FF)),
            PieSlice(id = "b", label = "B", value = 20f, color = Color(0xFF00E6A7)),
            PieSlice(id = "c", label = "C", value = 15f, color = Color(0xFFFF7A33)),
            PieSlice(id = "d", label = "D", value = 35f, color = Color(0xFFB049F8)),
        )
    )
    val style = PieStyle(
        layout = PieStyle.Layout(padding = 8.dp, startAngleDeg = -90f),
        arc = PieStyle.Arc(width = 20.dp, paddingAngleDeg = 2f, cornerRadius = 6.dp),
        labels = PieStyle.Labels(
            insideMinAngleDeg = 36f,
            outsideMinAngleDeg = 12f,
            textStyle = TextStyle(color = Color(0xFF333333)),
            labelPadding = 6.dp,
            formatter = { s, p -> "${'$'}{s.label} ${'$'}p%" }
        ),
        leaders = PieStyle.Leaders(show = true, lineWidth = 1.dp, offset = 6.dp)
    )
    PieChart(modifier = Modifier.fillMaxWidth().height(220.dp), data = data, style = style)
}


