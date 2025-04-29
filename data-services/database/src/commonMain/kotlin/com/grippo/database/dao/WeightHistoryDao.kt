package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.WeightHistoryEntity

@Dao
public interface WeightHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateWeightHistory(history: WeightHistoryEntity)

    @Query("SELECT * FROM weight_history ORDER BY createdAt")
    public suspend fun getWeightHistory(): List<WeightHistoryEntity>

    @Query("SELECT * FROM weight_history ORDER BY createdAt DESC LIMIT 1")
    public suspend fun getLastWeightHistory(): WeightHistoryEntity?

    @Query("DELETE FROM weight_history")
    public suspend fun deleteTableWeightHistory()
}