package com.grippo.services.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.grippo.services.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Dao
public interface GoalDao {

    @Query("SELECT * FROM goal WHERE userId = :userId LIMIT 1")
    public fun getByUserId(userId: String): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(entity: GoalEntity)

    @Update
    public suspend fun update(entity: GoalEntity)

    @Transaction
    public suspend fun insertOrUpdate(entity: GoalEntity) {
        val existing = getByUserId(entity.userId).firstOrNull()
        if (existing != null) {
            update(entity)
        } else {
            insert(entity)
        }
    }

    @Query("DELETE FROM goal WHERE userId = :userId")
    public suspend fun deleteByUserId(userId: String)
}
