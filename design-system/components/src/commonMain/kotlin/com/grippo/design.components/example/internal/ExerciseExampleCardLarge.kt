package com.grippo.design.components.example.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
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

@Composable
internal fun ExerciseExampleCardLarge(
    modifier: Modifier,
    value: ExerciseExampleState,
    allowUsageLabel: Boolean,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            ExerciseExampleImage(
                value = value.value.imageUrl,
                style = ExerciseExampleImageStyle.MEDIUM
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
            ) {
                Text(
                    text = value.value.name,
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                ) {
                    Chip(
                        label = ChipLabel.Empty,
                        value = value.value.category.title().text(),
                        size = ChipSize.Small,
                        stype = ChipStype.Default,
                        trailing = ChipTrailing.Empty,
                        contentColor = AppTokens.colors.static.white,
                        brush = SolidColor(value.value.category.color())
                    )

                    Chip(
                        label = ChipLabel.Empty,
                        value = value.value.forceType.title().text(),
                        size = ChipSize.Small,
                        stype = ChipStype.Default,
                        trailing = ChipTrailing.Empty,
                        contentColor = AppTokens.colors.static.white,
                        brush = SolidColor(value.value.forceType.color())
                    )

                    Chip(
                        label = ChipLabel.Empty,
                        value = value.value.weightType.title().text(),
                        size = ChipSize.Small,
                        stype = ChipStype.Default,
                        trailing = ChipTrailing.Empty,
                        contentColor = AppTokens.colors.static.white,
                        brush = SolidColor(value.value.weightType.color())
                    )
                }
            }
        }

        if (allowUsageLabel) {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val lastUsedDate = value.value.lastUsed?.let { l ->
                    DateCompose.rememberFormat(l.date, DateFormat.DateOnly.DateDdMmm)
                }

                lastUsedDate?.let {
                    Text(
                        text = AppTokens.strings.res(Res.string.last_used_label),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary
                    )

                    Spacer(modifier = Modifier.width(AppTokens.dp.contentPadding.text))

                    Text(
                        text = lastUsedDate,
                        style = AppTokens.typography.b12Semi(),
                        color = AppTokens.colors.text.secondary
                    )
                } ?: Text(
                    text = AppTokens.strings.res(Res.string.not_used_before),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.tertiary
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargePreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Large(allowUsageLabel = true),
        )

        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Large(allowUsageLabel = false),
        )
    }
}