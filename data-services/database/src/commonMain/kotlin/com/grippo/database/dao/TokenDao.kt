package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.TokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface TokenDao {

    @Query("SELECT * FROM token WHERE id = :id LIMIT 1")
    public fun getById(id: String): Flow<TokenEntity?>

    // ────────────── INSERT ──────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(value: TokenEntity)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM token WHERE id = :id")
    public suspend fun delete(id: String)
}