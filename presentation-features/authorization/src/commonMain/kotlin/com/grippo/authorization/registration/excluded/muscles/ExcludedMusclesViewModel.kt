package com.grippo.authorization.registration.excluded.muscles

import com.grippo.core.BaseViewModel

internal class ExcludedMusclesViewModel :
    BaseViewModel<ExcludedMusclesState, ExcludedMusclesDirection, ExcludedMusclesLoader>(
        ExcludedMusclesState()
    ), ExcludedMusclesContract {

    override fun next() {
        val direction = ExcludedMusclesDirection.MissingEquipment
        navigateTo(direction)
    }
}