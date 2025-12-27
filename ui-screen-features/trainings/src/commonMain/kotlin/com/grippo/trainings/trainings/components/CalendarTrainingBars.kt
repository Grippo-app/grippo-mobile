package com.grippo.trainings.trainings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import kotlin.math.min

private const val DefaultCalendarTrainingBars = 4

@Composable
internal fun CalendarTrainingBars(
    modifier: Modifier = Modifier,
    count: Int,
    barColor: Color,
    maxVisibleBars: Int = DefaultCalendarTrainingBars,
) {
    if (count <= 0) return

    val barsToShow = min(count, maxVisibleBars)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(barsToShow) { index ->
            key(index) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
            }
        }

        if (count > barsToShow) {
            Text(
                text = "+${count - barsToShow}",
                style = AppTokens.typography.b11Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}
