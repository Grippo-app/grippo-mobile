package com.grippo.design.components.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.examples.ExerciseExampleBundleState
import com.grippo.core.state.examples.ExerciseExampleComponentsState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleState
import com.grippo.core.state.muscles.stubMuscleGroup
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
import kotlinx.collections.immutable.toPersistentList
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
                        usageCount = 4,
                        lastUsed = DateTimeFormatState.Empty(DateFormat.DateOnly.DateDdMmm),
                    ),
                    equipments = persistentListOf(),
                    bundles = persistentListOf(),
                ),
            ),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    value = example.value.copy(
                        id = "exercise-example-without-equipment-data",
                        name = "Push-up",
                        weightType = WeightTypeEnumState.BODY_WEIGHT,
                        usageCount = 0,
                    ),
                    equipments = persistentListOf(),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargeSingleMuscleGroupPreview() {
    val example = exerciseExampleCardPreviewValue()
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    value = example.value.copy(name = "Згинання рук з гантелями"),
                    bundles = persistentListOf(
                        ExerciseExampleBundleState(
                            id = "biceps-involvement",
                            muscle = MuscleState(id = "biceps", type = MuscleEnumState.BICEPS),
                            percentage = PercentageFormatState.of(100),
                        ),
                    ),
                    equipments = persistentListOf(
                        EquipmentState(id = "dumbbells", type = EquipmentEnumState.DUMBBELLS),
                    ),
                ),
            ),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    value = example.value.copy(
                        id = "exercise-example-leg-extension",
                        name = "Розгинання ніг у тренажері",
                    ),
                    bundles = persistentListOf(
                        ExerciseExampleBundleState(
                            id = "quadriceps-involvement",
                            muscle = MuscleState(id = "quadriceps", type = MuscleEnumState.QUADRICEPS),
                            percentage = PercentageFormatState.of(100),
                        ),
                    ),
                    equipments = persistentListOf(
                        EquipmentState(id = "leg-extension", type = EquipmentEnumState.LEG_EXTENSION_MACHINES),
                    ),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargeAllMuscleGroupsPreview() {
    val example = exerciseExampleCardPreviewValue()
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    value = example.value.copy(
                        name = "Комплексна вправа на блочному тренажері зі змінними рукоятками",
                    ),
                    bundles = stubMuscleGroup().mapIndexed { index, group ->
                        ExerciseExampleBundleState(
                            id = "group-involvement-${group.id}",
                            muscle = group.muscles.first().value,
                            percentage = PercentageFormatState.of(if (index == 0) 50 else 10),
                        )
                    }.toPersistentList(),
                    equipments = persistentListOf(
                        EquipmentState(id = "pulldown", type = EquipmentEnumState.LAT_PULLDOWN),
                        EquipmentState(id = "wide-handle", type = EquipmentEnumState.WIDE_GRIP_HANDLE),
                        EquipmentState(id = "rope", type = EquipmentEnumState.ROPE),
                        EquipmentState(id = "cord-handles", type = EquipmentEnumState.CORD_HANDLES),
                    ),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargeWrappedImageGroupsPreview() {
    val example = exerciseExampleCardPreviewValue()
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    value = example.value.copy(name = "Вправа з різним обладнанням"),
                    bundles = persistentListOf(
                        example.bundles.first().copy(percentage = PercentageFormatState.of(40)),
                        example.bundles.last(),
                        ExerciseExampleBundleState(
                            id = "quadriceps-involvement",
                            muscle = MuscleState(id = "quadriceps", type = MuscleEnumState.QUADRICEPS),
                            percentage = PercentageFormatState.of(30),
                        ),
                    ),
                    equipments = listOf(
                        EquipmentEnumState.DUMBBELLS,
                        EquipmentEnumState.BARBELL,
                        EquipmentEnumState.ADJUSTABLE_BENCH,
                        EquipmentEnumState.ROPE,
                        EquipmentEnumState.WIDE_GRIP_HANDLE,
                        EquipmentEnumState.CLOSE_GRIP_HANDLE,
                        EquipmentEnumState.CORD_HANDLES,
                        EquipmentEnumState.LAT_PULLDOWN,
                    ).map { type ->
                        EquipmentState(id = type.name, type = type)
                    }.toPersistentList(),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargeUnavailableMuscleGroupsPreview() {
    val example = exerciseExampleCardPreviewValue()
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    bundles = example.bundles.map { bundle ->
                        bundle.copy(percentage = PercentageFormatState.of(0))
                    }.toPersistentList(),
                ),
            ),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                example.copy(
                    bundles = persistentListOf(
                        example.bundles.first().copy(percentage = PercentageFormatState.of(-1)),
                        example.bundles.last(),
                    ),
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
        bundles = persistentListOf(
            ExerciseExampleBundleState(
                id = "chest-involvement",
                muscle = MuscleState(id = "chest", type = MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL),
                percentage = PercentageFormatState.of(70),
            ),
            ExerciseExampleBundleState(
                id = "triceps-involvement",
                muscle = MuscleState(id = "triceps", type = MuscleEnumState.TRICEPS),
                percentage = PercentageFormatState.of(30),
            ),
        ),
        equipments = persistentListOf(
            EquipmentState(id = "dumbbells", type = EquipmentEnumState.DUMBBELLS),
            EquipmentState(id = "flat-bench", type = EquipmentEnumState.FLAT_BENCH),
        ),
    )
}
