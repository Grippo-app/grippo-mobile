package com.grippo.services.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.services.database.entity.WeightHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface WeightHistoryDao {

    @Query("SELECT * FROM weight_history ORDER BY createdAt DESC")
    public fun get(): Flow<List<WeightHistoryEntity>>

    @Query("SELECT * FROM weight_history ORDER BY createdAt DESC LIMIT 1")
    public fun getLast(): Flow<WeightHistoryEntity?>

    // ────────────── INSERT ──────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(history: WeightHistoryEntity)

    @Transaction
    public suspend fun replaceByUserId(userId: String, values: List<WeightHistoryEntity>) {
        deleteByUserId(userId)
        values.forEach { insertOrUpdate(it) }
    }

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM weight_history")
    public suspend fun delete()

    @Query("DELETE FROM weight_history WHERE userId = :userId")
    public suspend fun deleteByUserId(userId: String)
}
