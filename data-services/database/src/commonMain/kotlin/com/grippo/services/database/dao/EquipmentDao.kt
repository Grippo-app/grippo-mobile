package com.grippo.services.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.grippo.services.database.entity.EquipmentEntity
import com.grippo.services.database.entity.EquipmentGroupEntity
import com.grippo.services.database.models.EquipmentGroupWithEquipments
import kotlinx.coroutines.flow.Flow

@Dao
public interface EquipmentDao {

    @Transaction
    @Query("SELECT * FROM equipment_group")
    public fun get(): Flow<List<EquipmentGroupWithEquipments>>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrUpdate(
        groups: List<EquipmentGroupEntity>,
        equipments: List<EquipmentEntity>
    ) {
        deleteMissingEquipmentGroups(groups.map { it.id })
        deleteMissingEquipments(equipments.map { it.id })

        updateExistingEquipmentGroups(groups)
        updateExistingEquipments(equipments)

        insertEquipmentGroups(groups)
        insertEquipments(equipments)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertEquipmentGroups(groups: List<EquipmentGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertEquipments(equipments: List<EquipmentEntity>)

    @Update
    public suspend fun updateExistingEquipmentGroups(groups: List<EquipmentGroupEntity>)

    @Update
    public suspend fun updateExistingEquipments(equipments: List<EquipmentEntity>)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM equipment_group WHERE id NOT IN (:ids)")
    public suspend fun deleteMissingEquipmentGroups(ids: List<String>)

    @Query("DELETE FROM equipment WHERE id NOT IN (:ids)")
    public suspend fun deleteMissingEquipments(ids: List<String>)

    @Query("DELETE FROM equipment_group")
    public suspend fun delete()
}