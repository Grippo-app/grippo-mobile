package com.grippo.services.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.grippo.services.database.entity.TokenEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Dao
public interface TokenDao {

    @Query("SELECT * FROM token WHERE id = :id LIMIT 1")
    public fun getById(id: String): Flow<TokenEntity?>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrUpdate(token: TokenEntity) {
        val existing = getById(token.id).firstOrNull()
        if (existing == null) {
            insert(token)
        } else {
            update(token)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(value: TokenEntity)

    @Update
    public suspend fun update(value: TokenEntity)

    @Query(
        """
        UPDATE token SET access = :access, refresh = :refresh
        WHERE id = :id AND access IS :expectedAccess AND refresh IS :expectedRefresh
        AND EXISTS (SELECT 1 FROM user_active WHERE id = 0 AND userId = :id)
        """
    )
    public suspend fun updateIfCurrent(
        id: String,
        expectedAccess: String?,
        expectedRefresh: String?,
        access: String,
        refresh: String,
    ): Int

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM token WHERE id = :id")
    public suspend fun delete(id: String)

    @Query(
        """
        DELETE FROM token
        WHERE id = :id AND access IS :expectedAccess AND refresh IS :expectedRefresh
        AND EXISTS (SELECT 1 FROM user_active WHERE id = 0 AND userId = :id)
        """
    )
    public suspend fun deleteIfCurrent(
        id: String,
        expectedAccess: String?,
        expectedRefresh: String?,
    )
}
