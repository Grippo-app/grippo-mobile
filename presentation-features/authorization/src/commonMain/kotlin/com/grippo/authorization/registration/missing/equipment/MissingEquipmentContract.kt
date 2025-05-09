package com.grippo.authorization.registration.missing.equipment

internal interface MissingEquipmentContract {
    fun next()
    fun selectEquipment(id: String)
    fun selectGroup(id: String)

    companion object Empty : MissingEquipmentContract {
        override fun next() {}
        override fun selectEquipment(id: String) {}
        override fun selectGroup(id: String) {}
    }
}