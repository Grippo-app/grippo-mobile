package com.grippo.home.trainings

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.state.trainings.TrainingListValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class HomeTrainingsState(
    val date: DateRange = DateTimeUtils.thisDay(),
    val trainings: ImmutableList<TrainingListValue> = persistentListOf(),
)
