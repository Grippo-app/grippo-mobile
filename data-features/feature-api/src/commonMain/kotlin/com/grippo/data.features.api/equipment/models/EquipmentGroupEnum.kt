package com.grippo.data.features.api.equipment.models

public enum class EquipmentGroupEnum(private val key: String) {
    FREE_WEIGHT("free_weight"),
    MACHINES("machines"),
    CABLE_MACHINES("cable_machines"),
    BODY_WEIGHT("body_weight"),
    BENCHES_AND_RACKS("benches_and_racks");

    public companion object {
        public fun of(key: String?): EquipmentGroupEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}