package com.grippo.muscle.loading.details

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.data.features.api.metrics.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Training
import com.grippo.domain.state.metrics.toState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.coroutines.flow.onEach

public class MuscleLoadingDetailsViewModel(
    range: DateRange,
    trainingFeature: TrainingFeature,
    private val muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
) : BaseViewModel<MuscleLoadingDetailsState, MuscleLoadingDetailsDirection, MuscleLoadingDetailsLoader>(
    MuscleLoadingDetailsState(range = DateRangeFormatState.of(range))
), MuscleLoadingDetailsContract {

    init {
        trainingFeature
            .observeTrainings(range.from, range.to)
            .onEach(::provideMuscleLoad)
            .safeLaunch()
    }

    override fun onBack() {
        navigateTo(MuscleLoadingDetailsDirection.Back)
    }

    private suspend fun provideMuscleLoad(trainings: List<Training>) {
        if (trainings.isEmpty()) {
            update { it.copy(summary = null) }
            return
        }

        val summary = muscleLoadingSummaryUseCase
            .fromTrainings(trainings)
            .toState()

        update { it.copy(summary = summary) }
    }

    override fun onSelectMode(value: MuscleLoadingDetailsShowingMode) {
        update { it.copy(mode = value) }
    }
}
