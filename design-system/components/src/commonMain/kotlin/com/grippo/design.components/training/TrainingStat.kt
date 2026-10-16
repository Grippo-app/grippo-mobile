package com.grippo.design.components.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
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
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = value,
            style = AppTokens.typography.h5(),
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStatPreview() {
    PreviewContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            TrainingStat(
                modifier = Modifier.weight(1f),
                label = "Duration",
                value = AnnotatedString("00:45:12"),
                contentColor = AppTokens.colors.text.primary,
            )
            TrainingStat(
                modifier = Modifier.weight(1f),
                label = "Tonnage",
                value = AnnotatedString("5 628 кг"),
                contentColor = AppTokens.colors.text.primary,
            )
            TrainingStat(
                modifier = Modifier.weight(1f),
                label = "Reps",
                value = AnnotatedString("x73"),
                contentColor = AppTokens.colors.text.primary,
            )
        }
    }
}
