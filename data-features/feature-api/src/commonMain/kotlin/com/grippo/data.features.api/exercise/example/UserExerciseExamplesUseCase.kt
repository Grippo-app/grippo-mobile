package com.grippo.data.features.api.exercise.example

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.models.ExampleParams
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

    public fun execute(params: ExampleParams): Flow<List<ExerciseExample>> {
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
                    queries = params.queries,
                    sorting = ExampleSortingEnum.RecentlyUsed,
                    rules = rules,
                    page = params.page,
                    experience = rules.experience
                )
            }
    }
}
