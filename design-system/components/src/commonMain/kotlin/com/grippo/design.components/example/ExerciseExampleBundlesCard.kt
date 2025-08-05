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
import com.grippo.design.components.chart.PieChart
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.Trailing
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.exercise.examples.ExerciseExampleBundleState
import com.grippo.state.exercise.examples.stubExerciseExample
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
        value.sortedByDescending { it.percentage }.toPersistentList()
    }

    val preset = MuscleEngine.generatePreset(MuscleColorStrategy.ByUniqueColor(internalList))

    val (front, back) = MuscleEngine.generateImages(preset, internalList)

    val pie = remember(internalList) {
        internalList.map { it.muscle.type.color(preset) to it.percentage.toLong() }
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
                data = pie
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
                        label = item.muscle.name,
                        value = "${item.percentage}%",
                        trailing = Trailing.Content {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        item.muscle.type.color(preset),
                                        RoundedCornerShape(4.dp)
                                    ),
                            )
                        },
                        contentColor = AppTokens.colors.text.inverted,
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