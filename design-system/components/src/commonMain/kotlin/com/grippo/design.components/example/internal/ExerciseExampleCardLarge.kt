package com.grippo.design.components.example.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.muscle.MuscleMark
import com.grippo.design.components.muscle.MuscleMarkStyle
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

    val muscleGroups = remember(value.bundles) {
        if (value.bundles.any { (it.percentage.value ?: 0) < 0 }) {
            emptyList()
        } else {
            value.bundles
                .filter { (it.percentage.value ?: 0) > 0 }
                .map { it.muscle.type.group() }
                .distinct()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        if (muscleGroups.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                muscleGroups.forEach { group ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MuscleMark(group = group, style = MuscleMarkStyle.Inline)
                        Text(
                            text = group.title().text(),
                            style = AppTokens.typography.b12Med(),
                            color = AppTokens.colors.text.secondary,
                        )
                    }
                }
            }
        }
    }
}
