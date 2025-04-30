package com.grippo.data.features.api.equipment.models

public enum class EquipmentStatusEnum(private val key: String) {
    INCLUDED("included"),
    EXCLUDED("excluded"),
    UNIDENTIFIED("unidentified");

    override fun toString(): String {
        return key
    }

    public companion object {
        public fun of(key: String?): EquipmentStatusEnum {
            return entries.firstOrNull { it.key == key } ?: UNIDENTIFIED
        }
    }
}