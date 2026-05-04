package com.grippo.design.components.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun TrainingStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    contentColor: Color,
) {
    val tokens = AppTokens.dp.training
    val shape = RoundedCornerShape(tokens.radius)

    Row(
        modifier = modifier
            .height(tokens.height)
            .clip(shape)
            .background(AppTokens.colors.background.card, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(AppTokens.dp.accent.smallWidth)
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = tokens.horizontalPadding,
                    vertical = tokens.verticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(tokens.textSpacing)
        ) {
            Text(
                text = label,
                style = AppTokens.typography.b11Med(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = AppTokens.typography.b13Bold(),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingStatPreview() {
    PreviewContainer {
        TrainingStat(
            label = "Duration",
            value = "00:45",
            accentColor = AppTokens.colors.training.intensity.startColor,
            contentColor = AppTokens.colors.training.intensity.contentColor,
        )
        TrainingStat(
            label = "Tonnage",
            value = "5 628 кг",
            accentColor = AppTokens.colors.training.volume.startColor,
            contentColor = AppTokens.colors.training.volume.contentColor,
        )
        TrainingStat(
            label = "Reps",
            value = "x73",
            accentColor = AppTokens.colors.training.repetitions.startColor,
            contentColor = AppTokens.colors.training.repetitions.contentColor,
        )
    }
}
