package com.grippo.authorization.registration.missing.equipments

internal interface MissingEquipmentsContract {
    fun next()
    fun selectEquipment(id: String)
    fun selectGroup(id: String)

    companion object Empty : MissingEquipmentsContract {
        override fun next() {}
        override fun selectEquipment(id: String) {}
        override fun selectGroup(id: String) {}
    }
}