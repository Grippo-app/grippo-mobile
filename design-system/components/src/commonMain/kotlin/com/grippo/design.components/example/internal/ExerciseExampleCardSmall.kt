package com.grippo.design.components.example.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ExerciseExampleCardSmall(
    modifier: Modifier,
    value: ExerciseExampleValueState,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExerciseExampleImage(
            value = value.imageUrl,
            style = ExerciseExampleImageStyle.SMALL
        )

        Text(
            text = value.name,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardSmallPreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Small(
                stubExerciseExample().value,
            ),
        )
    }
}