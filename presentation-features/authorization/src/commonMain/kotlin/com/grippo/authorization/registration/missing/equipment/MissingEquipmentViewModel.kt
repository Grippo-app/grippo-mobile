package com.grippo.authorization.registration.missing.equipment

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature

internal class MissingEquipmentViewModel(
    private val equipmentFeature: EquipmentFeature
) : BaseViewModel<MissingEquipmentState, MissingEquipmentDirection, MissingEquipmentLoader>(
    MissingEquipmentState()
), MissingEquipmentContract {

    override fun next() {

    }
}