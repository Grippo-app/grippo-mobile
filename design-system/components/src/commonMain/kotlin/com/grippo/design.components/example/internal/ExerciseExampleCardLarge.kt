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
import androidx.compose.ui.graphics.SolidColor
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipInteraction
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipLeading
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.equipment.EquipmentCard
import com.grippo.design.components.equipment.EquipmentCardStyle
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.muscle.MuscleCard
import com.grippo.design.components.muscle.MuscleCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_example_in_history
import com.grippo.design.resources.provider.exercise_example_last_used_value
import com.grippo.design.resources.provider.exercise_example_not_used
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.toolkit.date.utils.DateCompose
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseExampleCardLarge(
    modifier: Modifier,
    value: ExerciseExampleState,
) {
    val muscleGroups = remember(value.bundles) {
        if (value.bundles.any { (it.percentage.value ?: 0) < 0 }) {
            persistentListOf()
        } else {
            value.bundles
                .filter { (it.percentage.value ?: 0) > 0 }
                .map { it.muscle.type.group() }
                .distinct()
                .toPersistentList()
        }
    }

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

            ExerciseExampleUsage(value = value.value)

            if (value.equipments.isNotEmpty() || muscleGroups.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    EquipmentCard(value = value.equipments, style = EquipmentCardStyle.Compact)
                    MuscleCard(value = muscleGroups, style = MuscleCardStyle.Compact)
                }
            }

            if (value.equipments.isEmpty()) {
                Text(
                    text = value.value.weightType.title().text(),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
    }
}

@Composable
private fun ExerciseExampleUsage(value: ExerciseExampleValueState) {
    val lastUsedValue = value.lastUsed.value
    val hasHistory = value.usageCount > 0 || lastUsedValue != null

    if (hasHistory) {
        val lastUsedDate = if (lastUsedValue != null) {
            DateCompose.rememberFormat(value = lastUsedValue, format = value.lastUsed.format)
        } else {
            ""
        }
        val label = if (lastUsedDate.isNotBlank()) {
            AppTokens.strings.res(Res.string.exercise_example_last_used_value, lastUsedDate)
        } else {
            AppTokens.strings.res(Res.string.exercise_example_in_history)
        }

        Chip(
            label = ChipLabel.Empty,
            value = label,
            interaction = ChipInteraction.Static,
            leading = ChipLeading.Icon(AppTokens.icons.Calendar),
            size = ChipSize.Small,
            textColor = AppTokens.colors.text.primary,
            iconColor = AppTokens.colors.icon.secondary,
            brush = SolidColor(AppTokens.colors.background.card),
        )
    } else {
        Text(
            text = AppTokens.strings.res(Res.string.exercise_example_not_used),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
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
                    EquipmentState(
                        id = "duplicate-handle",
                        type = EquipmentEnumState.WIDE_GRIP_HANDLE
                    ),
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
