package com.grippo.design.components.example.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.core.AppTokens

@Composable
internal fun ExerciseExampleCardSmall(
    modifier: Modifier,
    value: ExerciseExampleValueState,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseExampleImage(
            value = value.imageUrl,
            style = ExerciseExampleImageStyle.SMALL,
        )

        Text(
            modifier = Modifier.weight(1f),
            text = value.name,
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.primary,
        )
    }
}
