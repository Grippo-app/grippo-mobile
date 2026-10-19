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
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.components.equipment.EquipmentCard
import com.grippo.design.components.equipment.EquipmentCardStyle
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.muscle.MuscleMark
import com.grippo.design.components.muscle.MuscleMarkStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_example_last_used_value
import com.grippo.design.resources.provider.exercise_example_weight_type_value
import com.grippo.toolkit.date.utils.DateCompose
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ExerciseExampleCardLarge(
    modifier: Modifier,
    value: ExerciseExampleState,
) {
    val equipments = remember(value.equipments) {
        value.equipments.distinctBy { it.type }
    }

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

        if (equipments.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                equipments.forEach { equipment ->
                    EquipmentCard(value = equipment, style = EquipmentCardStyle.Inline)
                }
            }
        } else {
            Text(
                text = AppTokens.strings.res(
                    Res.string.exercise_example_weight_type_value,
                    value.value.weightType.title().text(),
                ),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
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

@AppPreview
@Composable
private fun ExerciseExampleEquipmentSummaryPreview() {
    val example = stubExerciseExample()
    PreviewContainer {
        ExerciseExampleCardLarge(
            modifier = Modifier.fillMaxWidth(),
            value = example.copy(
                value = example.value.copy(
                    id = "equipment-summary",
                    name = "Тяга верхнього блока широким хватом з довгою рукояткою",
                    imageUrl = null,
                    lastUsed = DateTimeFormatState.Empty(format = example.value.lastUsed.format),
                ),
                equipments = persistentListOf(
                    EquipmentState(id = "pulldown", type = EquipmentEnumState.LAT_PULLDOWN),
                    EquipmentState(id = "wide-handle", type = EquipmentEnumState.WIDE_GRIP_HANDLE),
                    EquipmentState(id = "duplicate-handle", type = EquipmentEnumState.WIDE_GRIP_HANDLE),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleMissingEquipmentPreview() {
    val example = stubExerciseExample()
    PreviewContainer {
        ExerciseExampleCardLarge(
            modifier = Modifier.fillMaxWidth(),
            value = example.copy(
                value = example.value.copy(
                    id = "equipment-missing",
                    name = "Віджимання від підлоги",
                    imageUrl = null,
                    weightType = WeightTypeEnumState.BODY_WEIGHT,
                    lastUsed = DateTimeFormatState.Empty(format = example.value.lastUsed.format),
                ),
                equipments = persistentListOf(),
                bundles = persistentListOf(),
            ),
        )
    }
}
