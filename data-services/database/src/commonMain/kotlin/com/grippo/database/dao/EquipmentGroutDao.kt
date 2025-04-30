package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.database.models.EquipmentGroupWithEquipments
import kotlinx.coroutines.flow.Flow

@Dao
public interface EquipmentGroutDao {

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    public suspend fun insertOrIgnore(equipmentGroup: EquipmentGroupEntity)

    @Transaction
    @Query("SELECT * FROM equipment_group")
    public fun get(): Flow<List<EquipmentGroupWithEquipments>>

    @Query("DELETE FROM equipment_group")
    public suspend fun delete()
}