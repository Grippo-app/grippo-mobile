package com.grippo.data.features.api.exercise.example

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

public class UserExerciseExamplesUseCase(
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature
) {
    public fun execute(
        queries: ExampleQueries,
        sorting: ExampleSortingEnum,
        page: ExamplePage
    ): Flow<List<ExerciseExample>> {
        return combine(
            flow = excludedEquipmentsFeature
                .observeExcludedEquipments()
                .map { items -> items.map { it.id }.toSet() },
            flow2 = excludedMusclesFeature
                .observeExcludedMuscles()
                .map { items -> items.map { it.id }.toSet() },
            transform = { equipmentIds, muscleIds ->
                UserExerciseExampleRules(
                    excludedEquipmentIds = equipmentIds,
                    excludedMuscleIds = muscleIds
                )
            }
        )
            .distinctUntilChanged()
            .flatMapLatest { rules ->
                exerciseExampleFeature.observeExerciseExamples(
                    queries = queries,
                    sorting = sorting,
                    rules = rules,
                    page = page,
                )
            }
    }
}