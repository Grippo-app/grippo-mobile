package com.grippo.design.components.example.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
internal fun ExerciseExampleCardLarge(
    modifier: Modifier,
    value: ExerciseExampleState,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseExampleImage(
                value = value.value.imageUrl,
                style = ExerciseExampleImageStyle.MEDIUM
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
            ) {
                Text(
                    text = value.value.name,
                    style = AppTokens.typography.h5(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                ExampleTypeSection(
                    modifier = Modifier.fillMaxWidth(),
                    value = value.value,
                    size = ChipSize.Small
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

        val lastUsed = value.value.lastUsed
        Text(
            text = when (lastUsed) {
                is DateTimeFormatState.Empty -> AppTokens.strings.res(Res.string.not_used_before)
                else -> "${AppTokens.strings.res(Res.string.last_used_label)} ${lastUsed.display}"
            },
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
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Large(
                stubExerciseExample(),
            ),
        )
    }
}
