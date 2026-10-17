package com.grippo.design.components.example.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_example_last_used_value
import com.grippo.toolkit.date.utils.DateCompose

@Composable
internal fun ExerciseExampleCardLarge(
    modifier: Modifier,
    value: ExerciseExampleState,
) {
    val equipmentText = value.equipments
        .distinctBy { it.type }
        .map { it.type.title().text() }
        .joinToString(separator = " · ")
        .ifBlank { value.value.weightType.title().text() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.Top,
    ) {
        ExerciseExampleImage(
            value = value.value.imageUrl,
            style = ExerciseExampleImageStyle.MEDIUM,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = value.value.name,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )

            Text(
                text = equipmentText,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )

            val lastUsed = value.value.lastUsed
            if (lastUsed !is DateTimeFormatState.Empty) {
                val lastUsedValue = lastUsed.value
                val lastUsedDate = if (lastUsedValue != null) {
                    DateCompose.rememberFormat(value = lastUsedValue, format = lastUsed.format)
                } else {
                    lastUsed.display
                }

                if (lastUsedDate.isNotBlank()) {
                    Text(
                        text = AppTokens.strings.res(Res.string.exercise_example_last_used_value, lastUsedDate),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }
    }
}
