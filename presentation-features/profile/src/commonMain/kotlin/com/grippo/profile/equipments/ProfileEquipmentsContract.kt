package com.grippo.profile.equipments

internal interface ProfileEquipmentsContract {
    fun onApply()
    fun onSelectEquipment(id: String)
    fun onSelectGroup(id: String)
    fun onBack()

    companion object Empty : ProfileEquipmentsContract {
        override fun onApply() {}
        override fun onSelectEquipment(id: String) {}
        override fun onSelectGroup(id: String) {}
        override fun onBack() {}
    }
}