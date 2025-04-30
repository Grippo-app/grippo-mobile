package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.WeightHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface WeightHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(histories: List<WeightHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(history: WeightHistoryEntity)

    @Query("SELECT * FROM weight_history ORDER BY createdAt")
    public fun get(): Flow<List<WeightHistoryEntity>>

    @Query("SELECT * FROM weight_history ORDER BY createdAt DESC LIMIT 1")
    public fun getLast(): Flow<WeightHistoryEntity?>

    @Query("DELETE FROM weight_history")
    public suspend fun delete()
}