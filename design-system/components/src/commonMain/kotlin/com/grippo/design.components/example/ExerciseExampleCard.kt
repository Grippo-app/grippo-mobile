package com.grippo.design.components.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.components.example.internal.ExerciseExampleCardLarge
import com.grippo.design.components.example.internal.ExerciseExampleCardMedium
import com.grippo.design.components.example.internal.ExerciseExampleCardSmall
import com.grippo.design.components.frames.FocusFrame
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import kotlinx.datetime.LocalDateTime

@Immutable
public sealed interface ExerciseExampleCardStyle {
    @Immutable
    public data class Small(
        val value: ExerciseExampleValueState,
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Medium(
        val value: ExerciseExampleState,
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Large(
        val value: ExerciseExampleState,
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
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardMediumPreview() {
    val example = exerciseExampleCardPreviewValue()
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Medium(example),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Medium(
                example.copy(
                    value = example.value.copy(
                        name = "Тяга верхнього блока до грудей широким хватом",
                    ),
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

        FocusFrame(modifier = Modifier.fillMaxWidth()) {
            ExerciseExampleCard(
                modifier = Modifier.fillMaxWidth(),
                style = ExerciseExampleCardStyle.Small(
                    example.copy(name = "Жим гантелей лежачи на похилій лаві нейтральним хватом"),
                ),
            )
        }
    }
}

private fun exerciseExampleCardPreviewValue(): ExerciseExampleState {
    val example = stubExerciseExample()
    return example.copy(
        value = example.value.copy(
            id = "exercise-example-used",
            lastUsed = DateTimeFormatState.Valid(
                display = "14 Oct",
                value = LocalDateTime(2026, 10, 14, 12, 0),
                format = DateFormat.DateOnly.DateDdMmm,
            ),
        ),
    )
}
