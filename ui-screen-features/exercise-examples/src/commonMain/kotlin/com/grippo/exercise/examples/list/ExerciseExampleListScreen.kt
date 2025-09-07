package com.grippo.exercise.examples.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.badge.Badge
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.inputs.InputSearch
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Filter
import com.grippo.design.resources.provider.trainings
import com.grippo.state.exercise.examples.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleListScreen(
    state: ExerciseExampleListState,
    loaders: ImmutableSet<ExerciseExampleListLoader>,
    contract: ExerciseExampleListContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
        onBack = contract::onBack,
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = AppTokens.dp.contentPadding.content,
                        start = AppTokens.dp.screen.horizontalPadding,
                        end = AppTokens.dp.screen.horizontalPadding
                    ),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                InputSearch(
                    modifier = Modifier.weight(1f),
                    value = state.query,
                    onValueChange = contract::onQueryChange
                )

                Box {
                    Button(
                        content = ButtonContent.Icon(
                            icon = AppTokens.icons.Filter
                        ),
                        style = ButtonStyle.Tertiary,
                        onClick = contract::onFiltersClick
                    )

                    val count = remember(state.filters) {
                        state.filters.count { it.isSelected() }
                    }
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        value = count
                    )
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.content,
            bottom = AppTokens.dp.screen.verticalPadding
        ),
    ) {
        items(
            items = state.exerciseExamples,
            key = { it.value.id },
        ) { item ->
            val clickProvider = remember(item) {
                { contract.onExerciseExampleClick(item.value.id) }
            }

            ExerciseExampleCard(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                value = item,
                style = ExerciseExampleCardStyle.Wide(onCardClick = clickProvider),
            )
        }
    }

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ExerciseExampleListScreen(
            state = ExerciseExampleListState(
                exerciseExamples = persistentListOf(stubExerciseExample(), stubExerciseExample())
            ),
            loaders = persistentSetOf(),
            contract = ExerciseExampleListContract.Empty
        )
    }
}
