package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.database.models.EquipmentGroupWithEquipments

@Dao
public interface EquipmentDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    public suspend fun insertOrUpdateEquipment(equipment: EquipmentEntity)

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    public suspend fun insertOrIgnoreEquipmentGroup(equipmentGroup: EquipmentGroupEntity)

    @Transaction
    @Query("SELECT * FROM equipment WHERE id IN (:ids)")
    public suspend fun getEquipmentById(ids: List<String>): List<EquipmentEntity>

    @Transaction
    @Query("SELECT * FROM equipment_group")
    public suspend fun getEquipmentGroups(): List<EquipmentGroupWithEquipments>

    @Query("DELETE FROM equipment")
    public suspend fun deleteTableEquipment()

    @Query("DELETE FROM equipment_group")
    public suspend fun deleteTableEquipmentGroup()
}