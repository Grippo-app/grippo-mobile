package com.grippo.home.trainings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.trainings
import com.grippo.presentation.api.trainings.models.stubTraining
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun HomeTrainingsScreen(
    state: HomeTrainingsState,
    loaders: ImmutableSet<HomeTrainingsLoader>,
    contract: HomeTrainingsContract
) = BaseComposeScreen(AppTokens.colors.background.secondary) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
    )

    val list = remember(state.trainings) {
        state
            .trainings
            .firstOrNull() // todo temporary solution, need to rework
            ?.exercises
            .orEmpty()
            .toPersistentList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {

        items(list, key = { it.id }) { exercise ->
            ExerciseCard(
                modifier = Modifier.fillMaxWidth(),
                value = exercise,
                onExerciseExampleClick = contract::openExerciseExample
            )
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        HomeTrainingsScreen(
            state = HomeTrainingsState(
                trainings = persistentListOf(stubTraining(), stubTraining()),
            ),
            loaders = persistentSetOf(),
            contract = HomeTrainingsContract.Empty
        )
    }
}