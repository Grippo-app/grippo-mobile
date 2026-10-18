package com.grippo.muscle.loading.details

import com.grippo.analytics.muscle.loading.details.MuscleLoadingDetailsAnalytics
import com.grippo.analytics.muscle.loading.details.models.MuscleLoadingDetailsEvent
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.data.features.api.metrics.distribution.MuscleLoadingSummaryUseCase
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadSummary
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.domain.state.metrics.distribution.toState
import com.grippo.muscle.loading.details.analytics.toAnalytics
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

public class MuscleLoadingDetailsViewModel(
    range: DateRange,
    private val analytics: MuscleLoadingDetailsAnalytics,
    trainingFeature: TrainingFeature,
    muscleLoadingSummaryUseCase: MuscleLoadingSummaryUseCase,
) : BaseViewModel<MuscleLoadingDetailsState, MuscleLoadingDetailsDirection, MuscleLoadingDetailsLoader>(
    MuscleLoadingDetailsState(range = DateRangeFormatState.of(range))
), MuscleLoadingDetailsContract {

    init {
        trainingFeature
            .observeTrainings(start = range.from, end = range.to)
            .flatMapLatest(muscleLoadingSummaryUseCase::observeFromTrainings)
            .onEach(::provideMuscleLoad)
            .safeLaunch()
    }

    private fun provideMuscleLoad(value: MuscleLoadSummary) {
        val summary = value.toState()

        update { it.copy(summary = summary) }
    }

    override fun onBack() {
        analytics.trackEvent(MuscleLoadingDetailsEvent.DismissRequested)
        navigateTo(MuscleLoadingDetailsDirection.Back)
    }

    override fun onSelectMode(value: MuscleLoadingDetailsShowingMode) {
        analytics.trackEvent(MuscleLoadingDetailsEvent.ModeSelected(value.toAnalytics()))
        update { it.copy(mode = value) }
    }
}
