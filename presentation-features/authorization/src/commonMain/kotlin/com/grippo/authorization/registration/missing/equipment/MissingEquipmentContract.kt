package com.grippo.authorization.registration.missing.equipment

internal interface MissingEquipmentContract {
    fun next()

    companion object Empty : MissingEquipmentContract {
        override fun next() {}
    }
}