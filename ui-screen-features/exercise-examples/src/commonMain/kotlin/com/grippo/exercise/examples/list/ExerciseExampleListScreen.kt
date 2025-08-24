package com.grippo.exercise.examples.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
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

                Button(
                    content = ButtonContent.Icon(
                        icon = AppTokens.icons.Filter
                    ),
                    onClick = contract::onFiltersClick
                )
            }
        }
    )
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
