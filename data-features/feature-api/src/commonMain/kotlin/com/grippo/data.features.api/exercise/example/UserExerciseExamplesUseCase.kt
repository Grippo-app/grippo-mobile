package com.grippo.data.features.api.exercise.example

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import com.grippo.data.features.api.user.UserFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

public class UserExerciseExamplesUseCase(
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val userFeature: UserFeature
) {
    public fun execute(
        queries: ExampleQueries,
        page: ExamplePage
    ): Flow<List<ExerciseExample>> {
        return combine(
            flow = excludedEquipmentsFeature
                .observeExcludedEquipments()
                .map { items -> items.map { it.id }.toSet() },
            flow2 = excludedMusclesFeature
                .observeExcludedMuscles()
                .map { items -> items.map { it.id }.toSet() },
            flow3 = userFeature
                .observeUser()
                .mapNotNull { items -> items?.experience },
            transform = { equipmentIds, muscleIds, experience ->
                UserExerciseExampleRules(
                    excludedEquipmentIds = equipmentIds,
                    excludedMuscleIds = muscleIds,
                    experience = experience
                )
            }
        )
            .distinctUntilChanged()
            .flatMapLatest { rules ->
                exerciseExampleFeature.observeExerciseExamples(
                    queries = queries,
                    sorting = ExampleSortingEnum.RecentlyUsed,
                    rules = rules,
                    page = page,
                    experience = rules.experience
                )
            }
    }
}