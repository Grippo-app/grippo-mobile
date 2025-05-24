package com.grippo.home.trainings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun TrainingsScreen(
    state: TrainingsState,
    loaders: ImmutableSet<TrainingsLoader>,
    contract: TrainingsContract
) = BaseComposeScreen {
    val list = remember(state.trainings) {
        state
            .trainings
            .firstOrNull()
            ?.exercises
            .orEmpty()
            .toPersistentList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.paddings.screenHorizontal,
            vertical = AppTokens.dp.paddings.screenVertical
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        items(list, key = { it.id }) { exercise ->
            ExerciseCard(
                modifier = Modifier.fillMaxWidth(),
                value = exercise
            )
        }
    }
}