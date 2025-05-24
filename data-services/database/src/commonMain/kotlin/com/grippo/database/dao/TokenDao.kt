package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.TokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface TokenDao {

    @Query("SELECT * FROM token LIMIT 1")
    public fun get(): Flow<TokenEntity?>

    // ────────────── INSERT ──────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(value: TokenEntity)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM token")
    public suspend fun delete()
}