package com.grippo.authorization.registration.missing.equipments

internal interface MissingEquipmentsContract {
    fun onNextClick()
    fun onBack()
    fun onEquipmentClick(id: String)
    fun onGroupClick(id: String)

    companion object Empty : MissingEquipmentsContract {
        override fun onNextClick() {}
        override fun onBack() {}
        override fun onEquipmentClick(id: String) {}
        override fun onGroupClick(id: String) {}
    }
}