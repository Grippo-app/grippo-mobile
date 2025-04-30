package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.EquipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface EquipmentDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    public suspend fun insertOrUpdate(equipment: EquipmentEntity)

    @Transaction
    @Query("SELECT * FROM equipment WHERE id IN (:ids)")
    public fun getById(ids: List<String>): Flow<List<EquipmentEntity>>

    @Query("DELETE FROM equipment")
    public suspend fun delete()
}