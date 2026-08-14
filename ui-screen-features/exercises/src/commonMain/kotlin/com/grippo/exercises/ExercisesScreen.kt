package com.grippo.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.example.ExerciseExampleCard
import com.grippo.design.components.example.ExerciseExampleCardStyle
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.exercises_empty
import com.grippo.design.resources.provider.icons.EmptyExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExercisesScreen(
    state: ExercisesState,
    loaders: ImmutableSet<ExercisesLoader>,
    contract: ExercisesContract,
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.exercises),
        style = ToolbarStyle.Transparent,
    )

    val isLoading = remember(loaders) { ExercisesLoader.Loading in loaders }

    if (state.items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                Loader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                )
            } else {
                EmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                    value = AppTokens.icons.EmptyExerciseExample,
                    text = AppTokens.strings.res(Res.string.exercises_empty),
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            contentPadding = PaddingValues(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.contentPadding.content,
            ),
        ) {
            items(
                items = state.items,
                key = { it.value.id },
            ) { item ->
                ExerciseExampleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clickable { contract.onExampleClick(item.value.id) },
                    style = ExerciseExampleCardStyle.Medium(value = item),
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ExercisesScreenPreview() {
    PreviewContainer {
        ExercisesScreen(
            state = ExercisesState(items = persistentListOf(stubExerciseExample())),
            loaders = persistentSetOf(),
            contract = ExercisesContract.Empty,
        )
    }
}
