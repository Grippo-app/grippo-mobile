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

    private fun provideExerciseExample(value: ExerciseExample?) {
        val exampleState = value?.toState()
        update {
            it.copy(
                example = exampleState,
                muscleLoadData = DSProgressData(items = emptyList()),
                muscleLoadMuscles = MuscleLoadBreakdown(entries = emptyList()),
            )
        }

        exampleState ?: return

        safeLaunch {
            val visualization = analytics.muscleLoadFromExample(exampleState)
            val progress = visualization.perGroup.asChart()

            update { current ->
                if (current.example?.value?.id != exampleState.value.id) current
                else current.copy(
                    muscleLoadData = progress,
                    muscleLoadMuscles = visualization.perMuscle,
                )
            }
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
