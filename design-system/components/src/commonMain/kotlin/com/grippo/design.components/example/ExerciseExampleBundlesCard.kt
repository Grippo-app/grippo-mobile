package com.grippo.design.components.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.components.chart.PieChart
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.percent
import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import com.grippo.state.exercise.examples.stubExerciseExample
import com.grippo.state.formatters.PercentageFormatState
import com.grippo.state.formatters.UiText
import com.grippo.state.muscles.factory.MuscleColorStrategy
import com.grippo.state.muscles.factory.MuscleEngine
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun ExerciseExampleBundlesCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<ExerciseExampleBundleState>
) {

    val internalList = remember(value) {
        value.sortedByDescending { it.percentage.value }.toPersistentList()
    }

    val preset = MuscleEngine.generatePreset(MuscleColorStrategy.ByScaleStops(internalList))

    val (front, back) = MuscleEngine.generateImages(preset, internalList)

    val percent = AppTokens.strings.res(Res.string.percent)

    val pie = remember(internalList) {
        DSPieData(
            slices = internalList.mapIndexedNotNull { idx, it ->
                val percentage = (it.percentage as? PercentageFormatState.Valid)
                    ?: return@mapIndexedNotNull null

                DSPieSlice(
                    id = "slice-$idx",
                    label = "${percentage.display}$percent",
                    value = percentage.value.toFloat(),
                    color = it.muscle.type.color(preset),
                )
            }
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {

        Row(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Image(
                modifier = Modifier.weight(1f),
                imageVector = front,
                contentDescription = null
            )

            PieChart(
                modifier = Modifier.weight(1f).aspectRatio(1f),
                data = pie,
            )

            Image(
                modifier = Modifier.weight(1f),
                imageVector = back,
                contentDescription = null
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            internalList.forEach { item ->
                val color = remember(item.id) {
                    item.muscle.type.color(preset)
                }

                key(item.muscle.id) {
                    Chip(
                        modifier = modifier,
                        label = ChipLabel.Text(UiText.Str(item.muscle.name)),
                        value = item.percentage.short(),
                        trailing = ChipTrailing.Content {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        item.muscle.type.color(preset),
                                        RoundedCornerShape(4.dp)
                                    ),
                            )
                        },
                        contentColor = AppTokens.colors.muscle.text,
                        brush = Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleBundlesCardPreview() {
    PreviewContainer {
        ExerciseExampleBundlesCard(
            value = stubExerciseExample().bundles
        )
    }
}