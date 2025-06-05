package com.grippo.profile.equipments

internal interface ProfileEquipmentsContract {
    fun apply()
    fun selectEquipment(id: String)
    fun selectGroup(id: String)

    companion object Empty : ProfileEquipmentsContract {
        override fun apply() {}
        override fun selectEquipment(id: String) {}
        override fun selectGroup(id: String) {}
    }
}