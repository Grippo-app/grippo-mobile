package com.grippo.design.components.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.components.example.internal.ExerciseExampleCardLarge
import com.grippo.design.components.example.internal.ExerciseExampleCardMedium
import com.grippo.design.components.example.internal.ExerciseExampleCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Immutable
public sealed interface ExerciseExampleCardStyle {
    @Immutable
    public data class Small(
        public val value: ExerciseExampleValueState,
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Medium(
        public val value: ExerciseExampleValueState,
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Large(
        public val value: ExerciseExampleState,
    ) : ExerciseExampleCardStyle
}

@Composable
public fun ExerciseExampleCard(
    modifier: Modifier = Modifier,
    style: ExerciseExampleCardStyle,
) {
    when (style) {
        is ExerciseExampleCardStyle.Small -> ExerciseExampleCardSmall(
            modifier = modifier,
            value = style.value,
        )

        is ExerciseExampleCardStyle.Medium -> ExerciseExampleCardMedium(
            modifier = modifier,
            value = style.value,
        )

        is ExerciseExampleCardStyle.Large -> ExerciseExampleCardLarge(
            modifier = modifier,
            value = style.value,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargePreview() {
    val example = exerciseExampleCardPreviewValue()
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(example),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    value = example.value.copy(
                        id = "exercise-example-unused",
                        name = "Жим гантелей лежачи на похилій лаві нейтральним хватом",
                        usageCount = 0,
                        lastUsed = DateTimeFormatState.Empty(DateFormat.DateOnly.DateDdMmm),
                    ),
                    equipments = persistentListOf(
                        EquipmentState(id = "dumbbells", type = EquipmentEnumState.DUMBBELLS),
                        EquipmentState(id = "adjustable-bench", type = EquipmentEnumState.ADJUSTABLE_BENCH),
                    ),
                ),
            ),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    value = example.value.copy(
                        id = "exercise-example-without-equipment",
                        name = "Push-up",
                        weightType = WeightTypeEnumState.BODY_WEIGHT,
                        lastUsed = DateTimeFormatState.Empty(DateFormat.DateOnly.DateDdMmm),
                    ),
                    equipments = persistentListOf(),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardMediumPreview() {
    val example = exerciseExampleCardPreviewValue().value
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Medium(example),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Medium(
                example.copy(
                    name = "Згинання рук із гантелями на похилій лаві з супінацією",
                    category = CategoryEnumState.ISOLATION,
                    forceType = ForceTypeEnumState.PULL,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardSmallPreview() {
    val example = exerciseExampleCardPreviewValue().value
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Small(example),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Small(
                example.copy(name = "Жим гантелей лежачи на похилій лаві нейтральним хватом"),
            ),
        )
    }
}

private fun exerciseExampleCardPreviewValue(): ExerciseExampleState {
    val example = stubExerciseExample()
    return example.copy(
        value = example.value.copy(
            id = "exercise-example-used",
            name = "Dumbbell bench press",
            imageUrl = null,
            description = "",
            experience = ExperienceEnumState.INTERMEDIATE,
            forceType = ForceTypeEnumState.PUSH,
            weightType = WeightTypeEnumState.FREE,
            category = CategoryEnumState.COMPOUND,
            usageCount = 4,
            lastUsed = DateTimeFormatState.of(
                value = LocalDateTime(2026, 10, 14, 12, 0),
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateDdMmm,
            ),
        ),
        components = ExerciseExampleComponentsState.External(required = true),
        bundles = persistentListOf(),
        equipments = persistentListOf(
            EquipmentState(id = "dumbbells", type = EquipmentEnumState.DUMBBELLS),
            EquipmentState(id = "flat-bench", type = EquipmentEnumState.FLAT_BENCH),
        ),
    )
}
