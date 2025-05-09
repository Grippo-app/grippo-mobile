package com.grippo.authorization.registration.missing.equipment

internal interface MissingEquipmentContract {
    fun next()
    fun select(id: String)

    companion object Empty : MissingEquipmentContract {
        override fun next() {}
        override fun select(id: String) {}
    }
}