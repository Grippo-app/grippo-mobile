package com.grippo.home.trainings

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.trainings.models.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class HomeTrainingsState(
    val trainings: ImmutableList<TrainingState> = persistentListOf()
)