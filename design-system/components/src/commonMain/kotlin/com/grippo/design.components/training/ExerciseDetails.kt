package com.grippo.design.components.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.information.InformationCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.ChevronRight
import com.grippo.design.resources.intensity
import com.grippo.design.resources.kg
import com.grippo.design.resources.percent
import com.grippo.design.resources.source
import com.grippo.design.resources.tonnage
import com.grippo.presentation.api.trainings.models.ExerciseState
import com.grippo.presentation.api.trainings.models.stubExercise

@Composable
public fun ExerciseDetails(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    onExerciseExampleClick: (id: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.exerciseDetails.horizontalPadding)
    ) {

        InformationCard(
            modifier = Modifier.fillMaxWidth(),
            label = AppTokens.strings.res(Res.string.tonnage),
            value = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = value.volume.toString(),
                        style = AppTokens.typography.b14Bold(),
                        color = AppTokens.colors.text.secondary
                    )

                    Text(
                        text = AppTokens.strings.res(Res.string.kg),
                        style = AppTokens.typography.b14Semi(),
                        color = AppTokens.colors.text.secondary
                    )
                }
            }
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = AppTokens.colors.divider.default
        )

        InformationCard(
            modifier = Modifier.fillMaxWidth(),
            label = AppTokens.strings.res(Res.string.intensity),
            value = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = value.intensity.toString(),
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

        value.exerciseExample?.let { example ->
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = AppTokens.colors.divider.default
            )

            InformationCard(
                modifier = Modifier.fillMaxWidth(),
                label = AppTokens.strings.res(Res.string.source),
                value = {
                    val clickProvider = remember(example) {
                        { onExerciseExampleClick.invoke(example.id) }
                    }

                    Button(
                        text = example.name,
                        endIcon = AppTokens.icons.ChevronRight,
                        style = ButtonStyle.Transparent,
                        onClick = clickProvider
                    )
                }
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseDetailsPreview() {
    PreviewContainer {
        ExerciseDetails(
            value = stubExercise(),
            onExerciseExampleClick = {}
        )
    }
}