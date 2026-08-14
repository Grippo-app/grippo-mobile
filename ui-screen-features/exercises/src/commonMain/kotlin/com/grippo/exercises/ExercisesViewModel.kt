package com.grippo.exercises

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleScope
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import kotlinx.coroutines.flow.onEach

public class ExercisesViewModel(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExercisesState, ExercisesDirection, ExercisesLoader>(ExercisesState()),
    ExercisesContract {

    init {
        exerciseExampleFeature.observeExerciseExamples(
            name = null,
            scope = ExampleScope.All(),
            sorting = ExampleSortingEnum.NewAdded,
            rules = UserExerciseExampleRules(
                excludedEquipmentIds = emptySet(),
                excludedMuscleIds = emptySet(),
                experience = ExperienceEnum.BEGINNER,
            ),
            page = ExamplePage.Chunk,
            experience = ExperienceEnum.BEGINNER,
        )
            .onEach(::provideExamples)
            .safeLaunch()

        safeLaunch(loader = ExercisesLoader.Loading) {
            exerciseExampleFeature.getExerciseExamples().getOrThrow()
        }
    }

    private fun provideExamples(list: List<ExerciseExample>) {
        update { it.copy(items = list.toState()) }
    }

    override fun onExampleClick(id: String) {
        dialogController.show(DialogConfig.ExerciseExample(id = id))
    }
}
