package com.grippo.design.components.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun TrainingStat(
    modifier: Modifier = Modifier,
    label: String,
    value: AnnotatedString,
    contentColor: Color,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Text(
            text = value,
            style = AppTokens.typography.h4(),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@AppPreview
@Composable
private fun TrainingStatPreview() {
    PreviewContainer {
        TrainingStat(
            label = "Duration",
            value = AnnotatedString("00:45"),
            contentColor = AppTokens.colors.text.primary,
        )
        TrainingStat(
            label = "Tonnage",
            value = AnnotatedString("5 628 кг"),
            contentColor = AppTokens.colors.text.primary,
        )
        TrainingStat(
            label = "Reps",
            value = AnnotatedString("x73"),
            contentColor = AppTokens.colors.text.primary,
        )
    }
}
