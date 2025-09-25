package com.grippo.exercise.example.exerciseexample

import com.grippo.calculation.AnalyticsApi
import com.grippo.calculation.models.MuscleLoadBreakdown
import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.domain.state.exercise.example.toState
import kotlinx.coroutines.flow.onEach

public class ExerciseExampleViewModel(
    id: String,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    stringProvider: StringProvider,
    colorProvider: ColorProvider,
) : BaseViewModel<ExerciseExampleState, ExerciseExampleDirection, ExerciseExampleLoader>(
    ExerciseExampleState()
), ExerciseExampleContract {

    private val analytics = AnalyticsApi(stringProvider, colorProvider)

    init {
        exerciseExampleFeature.observeExerciseExample(id)
            .onEach(::provideExerciseExample)
            .safeLaunch()

        safeLaunch {
            exerciseExampleFeature.getExerciseExampleById(id).getOrThrow()
        }
    }

    private suspend fun provideExerciseExample(value: ExerciseExample?) {
        val exampleState = value?.toState() ?: return

        val muscleLoad = analytics.muscleLoadFromExample(
            example = exampleState
        )

        val progress = muscleLoad.perGroup.asChart()

        update { current ->
            current.copy(
                example = exampleState,
                muscleLoadData = progress,
                muscleLoadSummary = muscleLoad,
            )
        }
    }

    override fun onDismiss() {
        navigateTo(ExerciseExampleDirection.Back)
    }

    private fun MuscleLoadBreakdown.asChart(): DSProgressData = DSProgressData(
        items = entries.map { entry ->
            DSProgressItem(
                label = entry.label,
                value = entry.value,
                color = entry.color,
            )
        }
    )
}
