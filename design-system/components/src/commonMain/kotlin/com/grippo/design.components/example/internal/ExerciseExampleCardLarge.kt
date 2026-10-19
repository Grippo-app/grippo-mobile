package com.grippo.design.components.example.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
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
import com.grippo.design.resources.provider.muscles.bodySplit
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

            if (equipments.isNotEmpty() || muscleGroups.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (equipments.isNotEmpty()) {
                        ExerciseExampleEquipmentImages(equipments = equipments)
                    }
                    if (muscleGroups.isNotEmpty()) {
                        ExerciseExampleMuscleImages(groups = muscleGroups)
                    }
                }
            }

            if (equipments.isEmpty()) {
                Text(
                    text = AppTokens.strings.res(
                        Res.string.exercise_example_weight_type_value,
                        value.value.weightType.title().text(),
                    ),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }

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

@Composable
private fun ExerciseExampleEquipmentImages(
    modifier: Modifier = Modifier,
    equipments: List<EquipmentState>,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        equipments.forEach { equipment ->
            EquipmentCard(value = equipment, style = EquipmentCardStyle.Compact)
        }
    }
}

@Composable
private fun ExerciseExampleMuscleImages(
    modifier: Modifier = Modifier,
    groups: List<MuscleGroupEnumState>,
) {
    val upperGroups = groups.filterNot { it == MuscleGroupEnumState.LEGS }
    val highlightColor = AppTokens.colors.muscle.summaryHighlight

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        when (upperGroups.size) {
            0 -> Unit
            1 -> {
                val group = upperGroups.single()
                val groupTitle = group.title().text()
                MuscleMark(
                    modifier = Modifier.semantics {
                        text = AnnotatedString(groupTitle)
                    },
                    group = group,
                    style = MuscleMarkStyle.Summary,
                    color = highlightColor,
                )
            }

            else -> {
                val muscleColors = AppTokens.colors.muscle
                val image = remember(upperGroups, muscleColors, highlightColor) {
                    val preset = MuscleEnumState.colorPreset(
                        fallback = muscleColors.compactInactive,
                        outline = muscleColors.outline,
                        background = muscleColors.compactBackground,
                        colorFor = { muscle ->
                            if (muscle.group() in upperGroups) highlightColor else muscleColors.compactInactive
                        },
                    )
                    bodySplit(preset)
                }
                val groupTitles = upperGroups.map { it.title().text() }

                Image(
                    modifier = Modifier
                        .size(AppTokens.dp.muscle.summaryMarkSize)
                        .semantics {
                            text = AnnotatedString(groupTitles.joinToString(separator = ", "))
                        },
                    imageVector = image,
                    contentDescription = null,
                )
            }
        }

        if (MuscleGroupEnumState.LEGS in groups) {
            val legsTitle = MuscleGroupEnumState.LEGS.title().text()
            MuscleMark(
                modifier = Modifier.semantics {
                    text = AnnotatedString(legsTitle)
                },
                group = MuscleGroupEnumState.LEGS,
                style = MuscleMarkStyle.Summary,
                color = highlightColor,
            )
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
