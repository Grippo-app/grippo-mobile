package com.grippo.design.components.example.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.example.ExampleTypeSection
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.last_used_label
import com.grippo.design.resources.provider.not_used_before
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.datetime.LocalDateTime

@Composable
internal fun ExerciseExampleCardLarge(
    modifier: Modifier,
    value: ExerciseExampleState,
) {
    val shape = RoundedCornerShape(AppTokens.dp.exerciseExampleCard.radius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTokens.colors.background.card)
            .border(
                width = AppTokens.dp.exerciseExampleCard.borderWidth,
                color = AppTokens.colors.border.default,
                shape = shape,
            )
            .padding(AppTokens.dp.exerciseExampleCard.contentPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseExampleImage(
                value = value.value.imageUrl,
                style = ExerciseExampleImageStyle.MEDIUM
            )

            Text(
                modifier = Modifier.weight(1f),
                text = value.value.name,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )
        }

        ExampleTypeSection(
            modifier = Modifier.fillMaxWidth(),
            value = value.value,
            size = ChipSize.Small,
        )

        val lastUsed = value.value.lastUsed
        val lastUsedText = when (lastUsed) {
            is DateTimeFormatState.Empty -> AppTokens.strings.res(Res.string.not_used_before)
            else -> {
                val lastUsedValue = lastUsed.value
                val lastUsedDate = if (lastUsedValue != null) {
                    DateCompose.rememberFormat(value = lastUsedValue, format = lastUsed.format)
                } else {
                    lastUsed.display
                }

                "${AppTokens.strings.res(Res.string.last_used_label)} $lastUsedDate"
            }
        }

        Text(
            text = lastUsedText,
            style = AppTokens.typography.b12Med(),
            color = if (lastUsed is DateTimeFormatState.Empty) {
                AppTokens.colors.text.tertiary
            } else {
                AppTokens.colors.text.secondary
            },
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargePreview() {
    val example = stubExerciseExample()
    val lastUsed = DateTimeFormatState.of(
        value = LocalDateTime(year = 2026, month = 10, day = 14, hour = 12, minute = 0),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateDdMmm,
    )
    val usedExample = example.copy(value = example.value.copy(lastUsed = lastUsed))

    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(value = usedExample),
        )
    }
}
