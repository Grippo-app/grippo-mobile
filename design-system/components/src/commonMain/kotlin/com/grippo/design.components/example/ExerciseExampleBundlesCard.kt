package com.grippo.design.components.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.information.InformationCard
import com.grippo.design.components.chart.PieChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.percent
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleBundleState
import com.grippo.presentation.api.exercise.example.models.stubExerciseExample
import com.grippo.presentation.api.muscles.factory.MuscleColorStrategy
import com.grippo.presentation.api.muscles.factory.MuscleEngine
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

    Column(modifier = modifier) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            front?.let {
                Image(
                    modifier = Modifier.weight(1f),
                    imageVector = front,
                    contentDescription = null
                )
            }

            back?.let {
                Image(
                    modifier = Modifier.weight(1f),
                    imageVector = back,
                    contentDescription = null
                )
            }

            PieChart(
                modifier = Modifier.weight(1f).aspectRatio(1f),
                data = pie
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        internalList.forEachIndexed { index, item ->
            InformationCard(
                modifier = Modifier.fillMaxWidth(),
                label = item.muscle.name,
                trailing = {
                    Spacer(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                item.muscle.type.color(preset),
                                RoundedCornerShape(4.dp)
                            ),
                    )
                    Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))
                },
                value = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = item.percentage.toString(),
                            style = AppTokens.typography.b14Bold(),
                            color = AppTokens.colors.text.secondary
                        )

                        Text(
                            text = AppTokens.strings.res(Res.string.percent),
                            style = AppTokens.typography.b14Semi(),
                            color = AppTokens.colors.text.secondary
                        )
                    }
                }
            )

            if (index < internalList.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTokens.colors.divider.primary
                )
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