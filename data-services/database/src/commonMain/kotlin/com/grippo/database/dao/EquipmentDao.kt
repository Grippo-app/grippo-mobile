package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.database.models.EquipmentGroupWithEquipments
import kotlinx.coroutines.flow.Flow

@Dao
public interface EquipmentDao {

    @Transaction
    @Query("SELECT * FROM equipment_group")
    public fun get(): Flow<List<EquipmentGroupWithEquipments>>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrReplace(
        groups: List<EquipmentGroupEntity>,
        equipments: List<EquipmentEntity>
    ) {
        insertEquipmentGroups(groups)
        insertEquipments(equipments)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertEquipments(equipments: List<EquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertEquipmentGroups(groups: List<EquipmentGroupEntity>)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM equipment_group")
    public suspend fun delete()
}