package com.grippo.exercise.example.picker

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.suggestion.AiSuggestionFeature
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.state.domain.example.toDomain
import com.grippo.state.filters.FilterValue
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

public class ExerciseExamplePickerViewModel(
    targetMuscleGroupId: String?,
    userExerciseExamplesUseCase: UserExerciseExamplesUseCase,
    muscleFeature: MuscleFeature,
    private val exampleFeature: ExerciseExampleFeature,
    private val aiSuggestionFeature: AiSuggestionFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExamplePickerState, ExerciseExamplePickerDirection, ExerciseExamplePickerLoader>(
    ExerciseExamplePickerState(manual = ManualQueries(selectedMuscleGroupId = targetMuscleGroupId))
), ExerciseExamplePickerContract {

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { state ->
                val manual = state.manual
                val suggestion = state.suggestion

                val name = when (suggestion) {
                    null -> manual.name.trim()
                    else -> suggestion.name
                }

                val weightType = if (suggestion == null) {
                    manual.filters
                        .filterIsInstance<FilterValue.WeightType>()
                        .firstOrNull()
                        ?.value?.toDomain()
                } else {
                    null
                }

                val forceType = if (suggestion == null) {
                    manual.filters
                        .filterIsInstance<FilterValue.ForceType>()
                        .firstOrNull()
                        ?.value?.toDomain()
                } else {
                    null
                }

                val category = if (suggestion == null) {
                    manual.filters
                        .filterIsInstance<FilterValue.Category>()
                        .firstOrNull()
                        ?.value?.toDomain()
                } else {
                    null
                }

                val muscleGroupId = if (suggestion == null) {
                    manual.selectedMuscleGroupId
                } else {
                    null
                }

                ExampleQueries(
                    name = name,
                    weightType = weightType,
                    forceType = forceType,
                    category = category,
                    muscleGroupId = muscleGroupId
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { queries ->
                userExerciseExamplesUseCase.execute(
                    queries = queries,
                    page = ExamplePage.First15
                )
            }
            .onEach(::provideExerciseExamples)
            .safeLaunch()
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        update { it.copy(manual = it.manual.copy(muscleGroups = suggestions)) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val list = value.toState()
        update { it.copy(exerciseExamples = list) }
    }

    override fun onQueryChange(value: String) {
        update {
            it.copy(
                manual = it.manual.copy(name = value),
                suggestion = null
            )
        }
    }

    override fun onFiltersClick() {
        val dialog = DialogConfig.FilterPicker(
            initial = state.value.manual.filters,
            onResult = { value ->
                update {
                    it.copy(
                        manual = it.manual.copy(filters = value.toPersistentList()),
                        suggestion = null
                    )
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onClearSuggestion() {
        update { it.copy(suggestion = null) }
    }

    override fun onMuscleGroupClick(id: String) {
        update {
            val value = if (it.manual.selectedMuscleGroupId == id) null else id
            it.copy(
                manual = it.manual.copy(selectedMuscleGroupId = value),
                suggestion = null
            )
        }
    }

    override fun onSuggestClick() {
        safeLaunch(loader = ExerciseExamplePickerLoader.SuggestExample) {
//            val result = aiSuggestionFeature
//                .predictExerciseExample()
//                .getOrThrow() ?: return@safeLaunch

//            val name = exampleFeature
//                .observeExerciseExample(result.id)
//                .firstOrNull()?.value?.name ?: return@safeLaunch

            update {
                it.copy(
                    suggestion = AiSuggestionQueries(
                        id = "result.id",
                        name = "name",
                        reason = "result.reason"
                    )
                )
            }
        }
    }

    override fun onExerciseExampleSelectClick(id: String) {
        val example = state.value.exerciseExamples.find { f -> f.value.id == id } ?: return
        navigateTo(ExerciseExamplePickerDirection.BackWithResult(example))
    }

    override fun onDismiss() {
        navigateTo(ExerciseExamplePickerDirection.Back)
    }
}
