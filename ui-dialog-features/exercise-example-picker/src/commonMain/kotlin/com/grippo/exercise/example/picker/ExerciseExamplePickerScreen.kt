package com.grippo.exercise.example.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.inputs.InputSearch
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Filter
import com.grippo.design.resources.provider.select_exercise
import com.grippo.state.exercise.examples.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExamplePickerScreen(
    state: ExerciseExamplePickerState,
    loaders: ImmutableSet<ExerciseExamplePickerLoader>,
    contract: ExerciseExamplePickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.select_exercise),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            InputSearch(
                modifier = Modifier.weight(1f),
                value = state.query,
                onValueChange = contract::onQueryChange
            )

            Button(
                content = ButtonContent.Icon(
                    icon = AppTokens.icons.Filter
                ),
                style = ButtonStyle.Tertiary,
                onClick = contract::onFiltersClick
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
        ) {
            items(
                items = state.exerciseExamples,
                key = { it.value.id },
            ) { item ->
                val detailsClickProvider = remember(item) {
                    { contract.onExerciseExampleDetailsClick(item.value.id) }
                }

                val selectClickProvider = remember(item) {
                    { contract.onExerciseExampleSelectClick(item.value.id) }
                }

                ExerciseExampleCard(
                    modifier = Modifier.width(220.dp),
                    value = item,
                    style = ExerciseExampleCardStyle.Square(
                        onCardClick = selectClickProvider,
                        onDetailsClick = detailsClickProvider
                    ),
                )
            }

            if (state.exerciseExamples.isEmpty()) {
               item {
                   Spacer(modifier = Modifier.size(220.dp))
               }
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ExerciseExamplePickerScreen(
            state = ExerciseExamplePickerState(
                exerciseExamples = persistentListOf(
                    stubExerciseExample(),
                    stubExerciseExample()
                ),
            ),
            loaders = persistentSetOf(),
            contract = ExerciseExamplePickerContract.Empty
        )
    }
}