package com.grippo.authorization.registration.excluded.muscles

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.domain.mapper.toState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class ExcludedMusclesViewModel(
    private val muscleFeature: MuscleFeature,
) : BaseViewModel<ExcludedMusclesState, ExcludedMusclesDirection, ExcludedMusclesLoader>(
    ExcludedMusclesState()
), ExcludedMusclesContract {

    init {
        muscleFeature
            .observeMuscles()
            .onEach(::provideMuscles)
            .catch { sendError(it) }
            .launchIn(coroutineScope)

        safeLaunch {
            muscleFeature.getPublicMuscles()
        }
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        println(suggestions.size.toString() + " SIZEEEEEEE")
        update { it.copy(suggestions = suggestions) }
    }

    override fun next() {
        val direction = ExcludedMusclesDirection.MissingEquipment
        navigateTo(direction)
    }
}